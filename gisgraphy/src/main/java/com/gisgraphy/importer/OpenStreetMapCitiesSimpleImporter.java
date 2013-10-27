/*******************************************************************************
 *   Gisgraphy Project 
 * 
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 * 
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   Lesser General Public License for more details.
 * 
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA
 * 
 *  Copyright 2008  Gisgraphy project 
 *  David Masclet <davidmasclet@gisgraphy.com>
 *  
 *  
 *******************************************************************************/
package com.gisgraphy.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.FlushMode;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Required;

import com.gisgraphy.domain.geoloc.entity.Adm;
import com.gisgraphy.domain.geoloc.entity.City;
import com.gisgraphy.domain.geoloc.entity.OpenStreetMap;
import com.gisgraphy.domain.geoloc.entity.ZipCode;
import com.gisgraphy.domain.repository.IAdmDao;
import com.gisgraphy.domain.repository.ICityDao;
import com.gisgraphy.domain.repository.IIdGenerator;
import com.gisgraphy.domain.repository.IOpenStreetMapDao;
import com.gisgraphy.domain.repository.ISolRSynchroniser;
import com.gisgraphy.domain.valueobject.GISSource;
import com.gisgraphy.domain.valueobject.GisFeatureDistance;
import com.gisgraphy.domain.valueobject.GisgraphyConfig;
import com.gisgraphy.domain.valueobject.NameValueDTO;
import com.gisgraphy.domain.valueobject.Pagination;
import com.gisgraphy.fulltext.FullTextSearchEngine;
import com.gisgraphy.fulltext.FulltextQuery;
import com.gisgraphy.fulltext.FulltextResultsDto;
import com.gisgraphy.fulltext.IFullTextSearchEngine;
import com.gisgraphy.fulltext.SolrResponseDto;
import com.gisgraphy.geocoloc.IGeolocSearchEngine;
import com.gisgraphy.geoloc.GeolocQuery;
import com.gisgraphy.geoloc.GeolocResultsDto;
import com.gisgraphy.helper.GeolocHelper;
import com.gisgraphy.helper.StringHelper;
import com.gisgraphy.street.StreetType;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Import the cities from an (pre-processed) openStreet map data file.
 * The goal of this importer is to cross information between geonames and Openstreetmap. 
 * Geonames has no concept of city but of populated place (That can be a city, suburb or other)
 * By cross the informations we can add shape and set a 'municipality' flag to identify city.
 * 
 * 
 * @author <a href="mailto:david.masclet@gisgraphy.com">David Masclet</a>
 */
public class OpenStreetMapCitiesSimpleImporter extends AbstractSimpleImporterProcessor {
	
    
    protected IIdGenerator idGenerator;
    
    protected ICityDao cityDao;
    
    protected IAdmDao admDao;
    
    protected ISolRSynchroniser solRSynchroniser;
    
    protected IFullTextSearchEngine fullTextSearchEngine;
    
    private static final Pattern pattern = Pattern.compile("(\\w+)\\s\\d+.*",Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#flushAndClear()
     */
    @Override
    protected void flushAndClear() {
    	cityDao.flushAndClear();
    }
    
    @Override
    protected void setup() {
        super.setup();
        //temporary disable logging when importing
        FullTextSearchEngine.disableLogging=true;
        logger.info("reseting Openstreetmap generatedId");
        idGenerator.sync();
    }
    

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#getFiles()
     */
    @Override
    protected File[] getFiles() {
	return ImporterHelper.listCountryFilesToImport(importerConfig.getOpenStreetMapCitiesDir());
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#getNumberOfColumns()
     */
    @Override
    protected int getNumberOfColumns() {
	return 10;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#processData(java.lang.String)
     */
    @Override
    protected void processData(String line) throws ImporterException {
	String[] fields = line.split("\t");
	String countrycode=null;
	String name=null;
	Point location=null;
	
	//
	// Line table has the following fields :
	// --------------------------------------------------- 
	//0: N|W|R; 1 id; 2 name; 3 countrycode; 4 :postcode 
	//5:population 6:location; 7 : shape ;8: place tag; 9 : is_in;
	//
	//
	checkNumberOfColumn(fields);
	
	/*find city by name and position
	 * if found 
	 * 	update source
	 *  set municipality
	 *  update shape
	 *  if not found
	 *  	
	*/
	// name
	if (!isEmptyField(fields, 2, false)) {
	    name=fields[1].trim();
	    if (name==null){
	    	return;
	    }
	}
	
	if (!isEmptyField(fields, 3, true)) {
	    countrycode=fields[3].trim();
	}
	if (!isEmptyField(fields, 6, false)) {
	    try {
		location = (Point) GeolocHelper.convertFromHEXEWKBToGeometry(fields[6]);
	    } catch (RuntimeException e) {
	    	logger.warn("can not parse location for "+fields[6]+" : "+e);
	    	return;
	    }
	}
	City city=null;
	SolrResponseDto  nearestCity = getNearestCity(location, name,countrycode);
	if (nearestCity != null ){
		city = cityDao.getByFeatureId(nearestCity.getFeature_id());
			if (city==null){
				city = createNewCity();
			}
	} else {
		city = createNewCity();
	}
	//populate new fields
	if(city.getPopulation()==null && !isEmptyField(fields, 5, false)){
		try {
			int population = Integer.parseInt(fields[5].trim());
			city.setPopulation(population);
		} catch (NumberFormatException e) {
			logger.error("can not parse population :"+fields[5]);
		}
	}
	if(!isEmptyField(fields, 4, false) && city.getZipCodes()!=null && !city.getZipCodes().contains(fields[4])){
			city.addZipCode(new ZipCode(fields[5]));
	}
	if(!isEmptyField(fields, 7, false)){
		try {
			Geometry shape = (Point) GeolocHelper.convertFromHEXEWKBToGeometry(fields[7]);
			city.setShape(shape);
		    } catch (RuntimeException e) {
		    	logger.warn("can not parse shape for "+fields[7]+" : "+e);
		    	return;
		    }
	}
	city.setMunicipality(true);
	if(!isEmptyField(fields, 9, false)){
		String is_in =fields[9].trim();
		int index =-1;
		String admname;
		if ((index = is_in.lastIndexOf(","))!=-1){
			admname = is_in.substring(index);
		} else {
			admname = is_in;
		}
		SolrResponseDto solrResponseDto= getAdm(admname,countrycode);
		if (solrResponseDto!=null){
			Adm adm = admDao.getByFeatureId(solrResponseDto.getFeature_id());
			if (adm!=null){
				city.setAdm(adm);
			}
		}
	}
	try {
		cityDao.save(city);
	} catch (ConstraintViolationException e) {
		logger.error("Can not save "+dumpFields(fields)+"(ConstraintViolationException) we continue anyway but you should consider this",e);
	}catch (Exception e) {
		logger.error("Can not save "+dumpFields(fields)+" we continue anyway but you should consider this",e);
	}

    }

	private City createNewCity() {
		City city;
		city = new City();
		city.setFeatureId(idGenerator.getNextFeatureId());
		city.setSource(GISSource.OPENSTREETMAP);
		return city;
	}


	protected SolrResponseDto getNearestCity(Point location, String name,String countryCode) {
		if (location ==null || name==null){
			return null;
		}
		FulltextQuery query = (FulltextQuery)new FulltextQuery(name).withPlaceTypes(new Class[]{City.class}).around(location).withPagination(Pagination.ONE_RESULT);
		if (countryCode != null){
			query.limitToCountryCode(countryCode);
		}
		FulltextResultsDto results = fullTextSearchEngine.executeQuery(query);
		if (results != null){
			for (SolrResponseDto solrResponseDto : results.getResults()) {
				return solrResponseDto;
			}
		}
		return null;
	}
	
	protected SolrResponseDto getAdm(String name,String countryCode) {
		if (name==null){
			return null;
		}
		FulltextQuery query = (FulltextQuery)new FulltextQuery(name).withPlaceTypes(new Class[]{Adm.class}).withPagination(Pagination.ONE_RESULT);
		if (countryCode != null){
			query.limitToCountryCode(countryCode);
		}
		FulltextResultsDto results = fullTextSearchEngine.executeQuery(query);
		if (results != null){
			for (SolrResponseDto solrResponseDto : results.getResults()) {
				return solrResponseDto;
			}
		}
		return null;
	}
    

	/* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldBeSkiped()
     */
    @Override
    public boolean shouldBeSkipped() {
    	return !importerConfig.isOpenstreetmapImporterEnabled();
    }
    
   


    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#setCommitFlushMode()
     */
    @Override
    protected void setCommitFlushMode() {
    	this.cityDao.setFlushMode(FlushMode.COMMIT);
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldIgnoreComments()
     */
    @Override
    protected boolean shouldIgnoreComments() {
    	return true;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldIgnoreFirstLine()
     */
    @Override
    protected boolean shouldIgnoreFirstLine() {
    	return false;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.IGeonamesProcessor#rollback()
     */
    public List<NameValueDTO<Integer>> rollback() {
    	List<NameValueDTO<Integer>> deletedObjectInfo = new ArrayList<NameValueDTO<Integer>>();
    	logger.info("reseting openstreetmap cities...");
    	//TODO only cities that have source openstreetmap
    	    deletedObjectInfo
    		    .add(new NameValueDTO<Integer>(City.class.getSimpleName(), 0));
    	resetStatus();
    	return deletedObjectInfo;
    }
    
    
    @Required
    public void setOpenStreetMapDao(ICityDao cityDao) {
        this.cityDao = cityDao;
    }
    
    @Override
    //TODO test
    protected void tearDown() {
    	super.tearDown();
    	FullTextSearchEngine.disableLogging=false;
    	String savedMessage = this.statusMessage;
    	try {
    		this.statusMessage = internationalisationService.getString("import.fulltext.optimize");
    		solRSynchroniser.optimize();
    	} finally {
    	    // we restore message in case of error
    	    this.statusMessage = savedMessage;
    	}
    }
    
    
   

    @Required
    public void setSolRSynchroniser(ISolRSynchroniser solRSynchroniser) {
        this.solRSynchroniser = solRSynchroniser;
    }

    @Required
    public void setIdGenerator(IIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Required
    public void setCityDao(ICityDao cityDao) {
		this.cityDao = cityDao;
	}

    @Required
	public void setFullTextSearchEngine(IFullTextSearchEngine fullTextSearchEngine) {
		this.fullTextSearchEngine = fullTextSearchEngine;
	}

    @Required
	public void setAdmDao(IAdmDao admDao) {
		this.admDao = admDao;
	}
    
    

    
}
