# OpenAM Auth Plugin For BankID Norway

## About OpenAM
OpenAM is an "all-in-one" access management solution that provides the following features in a single unified project:

+ Authentication
    - Adaptive 
    - Strong  
+ Single sign-on (SSO)
+ Authorization
+ Entitlements
+ Federation 
+ Web Services Security

OpenAM provides mobile support out of the box, with full OAuth 2.0 and OpenID Connect support - modern protocols that 
provide the most efficient method for developing secure native or HTML5 mobile applications optimized for bandwidth and 
CPU.

The project is led by ForgeRock who integrate the OpenAM, OpenIDM, OpenDJ, OpenICF, and OpenIG open source projects to 
provide a quality-assured Identity Platform. Support, professional services, and training are available for the Identity
 Platform, providing stability and safety for the management of your digital identities. 

To find out more about the services ForgeRock provides, visit [www.forgerock.com][commercial_site].

To view the OpenAM project page, which also contains all of the documentation, visit
 [https://forgerock.org/openam/][project_page]. 

For a great place to start, take a look at [Getting Started With OpenAM]
(https://forgerock.org/openam/doc/bootstrap/getting-started/index.html "Getting Started With OpenAM").

For further help and discussion, visit the [community forums][community_forum].

# About this Plugin.

OpenAM has a modular authentication framework, that allows the product to be extended. This plugin allows OpenAM to use BankID as a form of authentication. 


**INSERT FEATURE/BENEFIT DESCRIPTIONS HERE**

This module has been developed for use with OpenAM 13.
     
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

## License

This project is licensed under the Common Development and Distribution License (CDDL). The following text applies to 
both this file, and should also be included in all files in the project:

> The contents of this file are subject to the terms of the Common Development and  Distribution License (the License). 
> You may not use this file except in compliance with the License.  
>   
> You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the specific language governing 
> permission and limitations under the License.  
>  
> When distributing Covered Software, include this CDDL Header Notice in each file and include the License file at 
> legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header, with the fields enclosed by brackets [] 
> replaced by your own identifying information: "Portions copyright [year] [name of copyright owner]".  
>   
> Copyright 2016 ForgeRock AS. 

### Disclaimer
This software is provided 'as is' without warranty of any kind, either express or implied, including, but not limited to, the implied warranties of fitness for a purpose, or the warranty of non-infringement. Without limiting the foregoing, ForgeRock makes no warranty that:

i. the software will meet your requirements  
ii. the software will be uninterrupted, timely, secure or error-free
iii. the results that may be obtained from the use of the software will be effective, accurate or reliable  
iv. the quality of the software will meet your expectations  
v. any errors in the software obtained from ForgeRock servers web site will be corrected.

This software and its documentation:

vi. could include technical or other mistakes, inaccuracies or typographical errors. The BGS may make changes to the software or documentation made available on its web site.
vii. may be out of date, and ForgeRock makes no commitment to update such materials.
ForgeRock assumes no responsibility for errors or ommissions in the software or documentation available from its web site.

In no event shall ForgeRock be liable to you or any third parties for any special, punitive, incidental, indirect or consequential damages of any kind, or any damages whatsoever, including, without limitation, those resulting from loss of use, data or profits, whether or not the BGS has been advised of the possibility of such damages, and on any theory of liability, arising out of or in connection with the use of this software.

The use of this software is done at your own discretion and risk and with agreement that you will be solely responsible for any damage to your computer system or loss of data that results from such activities. No advice or information, whether oral or written, obtained by you from ForgeRock or from the ForgeRock web site shall create any warranty for the software.

## All the Links!
To save you sifting through the readme looking for 'that link'...

- [ForgeRock's commercial website][commercial_site]
- [ForgeRock's community website][community_site]
- [ForgeRock's BackStage server][backstage] 
- [OpenAM Project Page][project_page]
- [Community Forums][community_forum]
- [Enterprise Build Downloads][enterprise_builds]
- [Enterprise Documentation][enterprise_docs]
- [Nightly Build Downloads][nightly_builds]
- [Nightly Documentation][nightly_docs]
- [Central Project Repository][central_repo]
- [Issue Tracking][issue_tracking]
- [Contributors][contributors]
- [Coding Standards][coding_standards]
- [Contributions][contribute]
- [How to Buy][how_to_buy]

[commercial_site]: https://www.forgerock.com
[community_site]: https://www.forgerock.org
[backstage]: https://backstage.forgerock.com
[project_page]: https://forgerock.org/openam/
[community_forum]: https://forgerock.org/forum/fr-projects/openam/
[enterprise_builds]: https://backstage.forgerock.com/#!/downloads/OpenAM/OpenAM%20Enterprise#browse
[enterprise_docs]: https://backstage.forgerock.com/#!/docs/openam
[nightly_builds]: https://forgerock.org/downloads/openam-builds/
[nightly_docs]: https://forgerock.org/documentation/openam/
[central_repo]: https://stash.forgerock.org/projects/OPENAM
[issue_tracking]: http://bugster.forgerock.org/
[docs_project]: https://stash.forgerock.org/projects/OPENAM/repos/openam-docs/browse
[contributors]: https://stash.forgerock.org/plugins/servlet/graphs?graph=contributors&projectKey=OPENAM&repoSlug=openam&refId=all-branches&type=c&group=weeks
[coding_standards]: https://wikis.forgerock.org/confluence/display/devcom/Coding+Style+and+Guidelines
[how_to_buy]: https://www.forgerock.com/platform/how-buy/
[contribute]: https://forgerock.org/projects/contribute/
