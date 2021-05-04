# daner-face-search

Wolfram backed visual similiarity based face search. Implemented specifically for the DANER project.

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* The free (registration required) Wolfram Engine, which can be obtained from [Free Wolfram Engine for Developers](https://www.wolfram.com/engine/)

## Build & run

Build with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 8080 and the default Hello World service can be accessed at
<http://localhost:8080/daner-face-search/v1/hello>

The Swagger UI is available at <http://localhost:8080/daner-face-search/api/>, providing access to both the `v1` and the 
`devel` versions of the GUI. 

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.
