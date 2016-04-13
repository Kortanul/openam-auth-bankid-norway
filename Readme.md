# README

This README explains how to compile BankID Norway authentication module and then how to make it available to OpenAM
     13.0.0
     
## Directory structure
 
* src/main/java - contains all the java sources
* src/main/resources - extra files used to customize OpenAM
 * bankidnorway - wrapper file to handle a custom BankID callback
 * config - OpenAM authentication callback file
 * XUI - user interface for authentication module states
 * amBankIDNorway.properties - language specific translation (default language)
 * amBankIDNorwayService.xml - OpenAM service definition for authentication module
 
## External dependencies

BankID requires three external libraries to work. Two of them are publicly available and should be automatically 
downloaded by `maven`. These are bouncycastle jar files:

```
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcmail-jdk15</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk15</artifactId>
            <version>${bouncycastle.version}</version>
        </dependency>
```

Third library, the `bidjserver.5.1.3-dist`, you need to get directly from BankID Norway. Follow the instruction 
available in a [maven guide](http://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html) to add a 3rd party 
jar file to the local maven repository.
 
## How to compile
It is a `maven` project so simply run `mvn install` from the **openam-auth-bankid-norway** directory


## Deployment
### Authentication module files

Following commands assume that OpenAM is deployed in a tomcat container with the `/openam` as an application context. 

```
cp -r src/main/resources/XUI <TOMCAT_WEBAPPS_DIR>/openam/
cp -r src/main/resources/bankidnorway <TOMCAT_WEBAPPS_DIR>/openam/
cp -r src/main/resources/config <TOMCAT_WEBAPPS_DIR>/openam/
cp target/openam-auth-bankid-norway-1.0.0-SNAPSHOT.jar <TOMCAT_WEBAPPS_DIR>/openam/WEB-INF/lib/
```

### Register authentication module

Follow the instructions available in the [OpenAM Developers Guide](https://backstage.forgerock.com/#!/docs/openam/13/dev-guide/chap-customizing#installing-sample-auth-module).

In step 2 use:
- `src/main/resources/amBankIDNorwayService.xml` file to create service
- `org.forgerock.openam.auth.bankid.BankIDNorway` as a class name to register module

