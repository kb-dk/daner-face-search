# daner-face-search

Wolfram backed visual similiarity based face search. Implemented specifically for the DANER project.

Developed and maintained by the Royal Danish Library.

## Requirements

* Maven 3                                  
* Java 11
* Wolfram Engine
* Trained features from the DANER project (kb.dk-internal for now, sorry)

### Wolfram Engine  

The free (registration required) Wolfram Engine which can be obtained from [Free Wolfram Engine for Developers](https://www.wolfram.com/engine/).

After registration, download and installation, remember to run
```
wolframscript
```
to activate the product.

### Trained features

Get the file `extracted-features-2021-03-22T13.59.09.mx` or later (ask Per or Toke) and put it in the daner-face-search
root folder.

Hopefully the features file will be uploaded somewhere public at some point.

## Build & run

Fetch the ML models needed by the application by running
```
wolframscript -file init_daner_script.wls
```
This only needs to be done once as the models are cached indefinitely.


Install `JLINK` from Wolfram in the local Maven cache:
```
mvn install:install-file -Dfile=/usr/local/Wolfram/WolframEngine/12.2/SystemFiles/Links/JLink/JLink.jar -DgroupId=com.wolfram -DartifactId=feature -Dversion=1.0.0 -Dpackaging=jar
```
The path might differ from the snippet above.



Build the Java service with
``` 
mvn package
```

Test the webservice with
```
mvn jetty:run
```

The default port is 8234 and the ping service can be accessed at
<http://localhost:8234/daner-face-search/v1/ping>

The Swagger UI is available at <http://localhost:8234/daner-face-search/api/>. 

### Deploy

See the file [DEVELOPER.md](DEVELOPER.md) for developer specific details and how to deploy to tomcat.

Special note: The support files for the Wolfram driven classification also needs to
be downloaded once on the deploy server. This is done automatically on first start,
but can take quite a while.

Running the classification download script performs this step and might be preferable:

```
wolframscript -file init_daner_script.wls
```
where the `init_daner_script.wls` is taken from the repository.   
