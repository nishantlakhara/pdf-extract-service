Github repository: 
https://github.com/nishantlakhara/pdf-extract-service/tree/master

Steps to run:
-------------
1. Download distributed hazelcast cache server.
   https://hazelcast.com/open-source-projects/downloads/
   Download the zip version of file.
   
   Note: The purpose of using the hazelcast distributed cache was to store the status of the currently processed file across the multiple instances of the same code base i.e sharing across multiple tomcat instances.
   
   Also the hazelcast cache supports synchronization like in a ConcurrentHashMap.

2. Unzip it. Run the hazelcast caching server using the script 
   bin/start.bat
   

3. Similarly start the management centre i.e the hazelcast UI.
   management-center/bin/start.bat
   
   The UI can be opened at http://localhost:8080
   More documentation can be seen on page:
   https://hazelcast.org/imdg/get-started/
   

4. Start the pdf-extract-service.
   Open the project in intellij and do a maven build.
   Run the java main class: PDFExtractServiceApplication.java
   
   The server is configured to run on port 8083. 
   It can be changed in the application.yml file with property server.port
   


APIs can be tested using the POSTMAN tool:
------------------------------------------
1. /pdf-extract/upload-file?file=<filepath>
2. /pdf-extract/get-status?filename=PDF32000_2008.pdf
   To get the current status of the pdf file being processed.
   

application.yml properties:
---------------------------
There are many parameters that can be manipulated as per the user requirements and the machine power.
I have tested it on a simple laptop with 2GB of java heap space.
The parameters can be found in application.yml file.
Please feel free to play around with them to get the best possible throughput.

The parameters are in the pdf-text-service property.
Also the max tomcat threads can be increased as per the machine power.

Scenarios:
----------
1. Upload file:
   Once file uploaded 
   
      -> HttpStatus 
   
      -> SEE_OTHER(303)
   
      -> Redirect to /get-status?filename=?
   

2. Get Status:
   Get status is in PROCESSING_COMPLETED
      -> HttpStatus
   
      -> SEE_OTHER(303)
   
      -> Redirect to /get-extracted-data?filename=?
   
      -> Return json text response so parsed using pdfbox 200 OK
   

3. Get Status:
   Get status is not PROCESSING_COMPLETED
   
      -> HttpStatus
   
      -> Accepted (202) with latest status.
   
   You need to again run this api after some time to check the latest processing status.

There are more cases like not found and no content.
Please refer rest api for that.
Please mail me on nishant.lakhara.280389@gmail.com for any explanation or queries.

Testing:
--------

Using Jmeter Thread Group

1. Tested with 500 concurrent threads with average pdf size of 2MB.
   Fast and works with 50 thread pool size.
2. Tested with 500 concurrent threads with average pdf size 20MB.
   Slower than point 1 and work with 20 thread pool size.
   
   Note: If I increase thread pool size, due to machine power i.e my PC,
   it runs out of resources for case 2.


   
   




    