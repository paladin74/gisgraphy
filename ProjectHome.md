Gisgraphy is a free, open source framework that offers the possibility to do geolocalisation and geocoding via Java APIs or REST webservices. Because geocoding is nothing without data, it provides an easy to use importer that will automagically download and import the necessary (free) data to your local database (Geonames and OpenStreetMap : 42 million entries). You can also add your own data with the Web interface or the importer connectors provided. Gisgraphy is production ready, and has been designed to be scalable(load balanced), performant and used in other languages than just java : results can be output in XML, JSON, PHP, Python, Ruby, YAML, GeoRSS, and Atom. One of the most popular GPS tracking System (OpenGTS) also includes a Gisgraphy client...read more . Here are the main functionalities :

  * geocoder / reverse geocoder

  * client lib / android application ([[gisgraphoid](http://code.google.com/p/gisgraphoid/)])

  * Importers for Openstreetmap data in csv format

  * Importers from geonames CSV files, reporting of inconsistencies and a report when import is complete. Just give the country(ies) you wish to import and / or the [placetypes](http://www.gisgraphy.com/placetype.php), and Gisgraphy download the files  and import them with all the alternateNames (optional), and sync the database with the fulltext search engine



  * REST Web Services

  * Full text search  (based on [Lucene](http://lucene.apache.org/java/) / [Solr](http://lucene.apache.org/solr/)  with default filters optimized for city search
> (case insensitivity, separator characters stripping, ..) via an Java API or a webservice

  * An admin interface

  * Fully replicated / scalable / high performance / cached services

  * Findnearby function (with limits, pagination, restrict to a specific country and/or language and other useful options) via a Java API or a Web Service

  * Search for zipcode or name

  * Several output formats supported : XML, json, php, ruby, python, ATOM, GEORSS...

  * Dojo widgets / prototype / Ajax to ease search but can be use it even if javascript is not enabled on the client side

  * Plateform / language independent because of webservices

  * Provides all the countries flags in svg and png format

  * more...

Note that Gisgraphy is under the [LGPL](http://www.gisgraphy.com/license.html) license V3 and the Geonames data are under the [creative commons attributions license](http://creativecommons.org)