[![Build Status](http://wingsbuild:0db28aa0f4fc0685df9a216fc7af0ca96254b7c2@ec2-54-174-51-35.compute-1.amazonaws.com/job/portal/buildStatus/icon)](http://wingsbuild:0db28aa0f4fc0685df9a216fc7af0ca96254b7c2@ec2-54-174-51-35.compute-1.amazonaws.com/job/portal/)

Wings Project Setup instructions :
1) Maven download : https://maven.apache.org/download.cgi 

2) Project Checkout from repository:  https://github.com/wings-software/wings

3) Set up Project:
    Intellij Setup:
    a. Import portal in intellij as maven project. 
    b. Import codeStyle/intellij-java-google-style.xml in intellij Settings/Editor/CodeStyle/Manage.

    To build project along with ui:
    a. Install npm: https://github.com/nodesource/distributions.
    b. cd to wings directory and checkout UI project from repository: https://github.com/wings-software/wingsui
    c. Run "mvn package -DbuildUI=true"


    To run api server run class WingsApplication with following arguments.
       VM Args: -Xbootclasspath/p:<Your home directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.8.v20160420/alpn-boot-8.1.8.v20160420.jar  
       Program Args: server config.yml
       Working Directory: $MODULE_DIR$

    To delegate, run class DelegateApplication with following arguments.
       VM Args: -Xbootclasspath/p:<Your home directory>/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.8.v20160420/alpn-boot-8.1.8.v20160420.jar -Dversion=999.0.0
       Program Args: config-delegate.yml
       Working Directory: $MODULE_DIR$

7) to apply database migrations run following command in dbmigrations folder:
   "mvn clean compile exec:java"
