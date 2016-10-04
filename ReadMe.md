Crawler
===================================

To build this project use

    mvn install

To run this project

    mvn exec:java -Dexec.args="http://website.to.crawl [output_file]"
	
	If the optional output file is not specified, the resulting site map html would be written
	to the standard output
	
To run tests

    mvn test
	
The application running time is limited to 59 seconds, so for a big web site it might terminate without 
producing a site map.
Dynamic web sites (which use java script to generate contents) are not supported.
The application would not recover from connection problems e.g. connection and access permission errors are not ignored.