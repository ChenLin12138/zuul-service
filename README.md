# zuul-service
Spring Cloud Learning zuul-service
## What Is It?
Spring cloud components learning.
## Spring Cloud components
### Spring boot 2.1.4.RELEASE
### zuul
## Download & Installation
1. Download a Java SE Runtime Environment (JRE),release version 8 or later, from http://www.oracle.com/technetwork/java/javase/downloads/index.html
2. Download Apache maven
Download Apache maven 3.6.0 here
http://maven.apache.org/download.cgi
3. Set Java Home or JRE Home.
4. Set Maven Home.
5. Import project as Existing maven projects.
6. Use mvn clean install to install this project.
## Demo
url调用
```
localhost:5555/actuator/routes
```
调用结果
```
{
    "/organization/**": "organizationservice",
    "/organizationservice/**": "organizationservice",
    "/licenseingservice/**": "licenseingservice"
}
```