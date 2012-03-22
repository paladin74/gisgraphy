package com.gisgraphy.fulltext;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.gisgraphy.domain.valueobject.Output;
import com.gisgraphy.domain.valueobject.Output.OutputStyle;
import com.gisgraphy.domain.valueobject.Pagination;
import com.gisgraphy.geocoloc.GeolocClient;
import com.gisgraphy.geoloc.GeolocQuery;
import com.gisgraphy.serializer.common.OutputFormat;
import com.gisgraphy.servlet.GisgraphyServlet;
import com.gisgraphy.test.GisgraphyUtilsTestHelper;

public class FulltextClientTest {

	private static final String HTTP_LOCALHOST_8080_GEOLOC = "http://localhost:8080/geoloc";

	@Test
	public void constructorShouldNotAcceptNullBaseUrl() {
		try {
			new FulltextClient(null);
			Assert.fail("constructor should not accept null base url");
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	@Test
	public void constructorShouldNotAcceptEmptyBaseUrl() {
		try {
			new FulltextClient(" ");
			Assert.fail("constructor should not accept empty base URL");
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	@Test
	public void constructorShouldNotAcceptWrongBaseUrl() {
		try {
			new FulltextClient("foo");
			Assert.fail("constructor should not accept wrong base URL");
		} catch (IllegalArgumentException e) {
			// ignore
		}
	}

	@Test
	public void constructorShouldAccepthttpsBaseUrl() {
		new FulltextClient("https://localhost:8080/geocoding");
	}

//	@Test
	public void constructorShouldAccepthttpBaseUrl() {
		new FulltextClient(HTTP_LOCALHOST_8080_GEOLOC);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void executeQueryWithNullQuery(){
		FulltextClient client = new FulltextClient(HTTP_LOCALHOST_8080_GEOLOC);
		client.executeQuery(null);
	}
	
	@Test(expected=RuntimeException.class)
	public void executeQueryToDatabaseObjects(){
		FulltextClient client = new FulltextClient(HTTP_LOCALHOST_8080_GEOLOC);
		client.executeQueryToDatabaseObjects(new FulltextQuery("test"));
	}
	
	@Test
	public void geolocQuerytoQueryString(){
		FulltextQuery query = createQuery();
		FulltextClient client = new FulltextClient(HTTP_LOCALHOST_8080_GEOLOC);
		String queryString = client.fulltextQueryToQueryString(query);
		HashMap<String, String> params = splitURLParams(queryString,"&");
		Assert.assertEquals(query.getQuery(), params.get(FulltextQuery.QUERY_PARAMETER));
		Assert.assertEquals(query.getLatitude().toString(), params.get(FulltextQuery.LAT_PARAMETER));
		Assert.assertEquals(query.getLongitude().toString(), params.get(FulltextQuery.LONG_PARAMETER));
		Assert.assertEquals(query.getRadius()+"", params.get(FulltextQuery.RADIUS_PARAMETER));
		Assert.assertEquals(query.isAllwordsRequired()+"", params.get(FulltextQuery.ALLWORDSREQUIRED_PARAMETER));
		Assert.assertEquals(query.getFirstPaginationIndex()+"", params.get(GisgraphyServlet.FROM_PARAMETER));
		Assert.assertEquals(query.getLastPaginationIndex()+"", params.get(GisgraphyServlet.TO_PARAMETER));
		Assert.assertEquals(query.getOutputFormat()+"", params.get(GisgraphyServlet.FORMAT_PARAMETER));
		Assert.assertEquals(query.getOutputLanguage(), params.get(FulltextQuery.LANG_PARAMETER));
		Assert.assertEquals(query.getOutputStyle()+"", params.get(FulltextQuery.STYLE_PARAMETER));
		//Assert.assertEquals(query.getPlaceTypes()[0].getSimpleName(), params.get(GeolocQuery.PLACETYPE_PARAMETER));
		Assert.assertEquals(query.getApikey()+"", params.get(GisgraphyServlet.APIKEY_PARAMETER));
		Assert.assertEquals(query.isOutputIndented()+"", params.get(GisgraphyServlet.INDENT_PARAMETER));
		Assert.assertEquals(query.getCountryCode()+"", params.get(FulltextQuery.COUNTRY_PARAMETER));
		Assert.assertEquals(query.isSpellcheckingEnabled()+"", params.get(FulltextQuery.SPELLCHECKING_PARAMETER));
		
	}
	
	 private FulltextQuery createQuery() {
		 FulltextQuery query = new FulltextQuery("paris");
		 query.around(GisgraphyUtilsTestHelper.createPoint(2.349F, 48.853F));
		 query.withRadius(1000);
		 query.withAllWordsRequired(true);
		 query.withPagination(Pagination.paginate().from(1).to(5));
		 query.withOutput(Output.withFormat(OutputFormat.JSON).withLanguageCode("FR").withStyle(OutputStyle.FULL).withIndentation());
		 query.setApikey("123");
		 query.limitToCountryCode("FR");
		 //todo placetype
		 query.withoutSpellChecking();
		 
		 return query;
	 }

	private static HashMap<String, String> splitURLParams(String completeURL,
			    String andSign) {
			int i;
			HashMap<String, String> searchparms = new HashMap<String, String>();
			;
			i = completeURL.indexOf("?");
			if (i > -1) {
			    String searchURL = completeURL
				    .substring(completeURL.indexOf("?") + 1);

			    String[] paramArray = searchURL.split(andSign);
			    for (int c = 0; c < paramArray.length; c++) {
				String[] paramSplited = paramArray[c].split("=");
				try {
				    searchparms.put(paramSplited[0], java.net.URLDecoder
					    .decode(paramSplited[1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				    return new HashMap<String, String>();
				}

			    }
			    // dumpHashtable;
			    java.util.Iterator<String> keys = searchparms.keySet().iterator();
			    while (keys.hasNext()) {
				String s = (String) keys.next();
			    }

			}
			return searchparms;
		    }


}