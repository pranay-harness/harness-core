[![Build Status](http://wingsbuild:0db28aa0f4fc0685df9a216fc7af0ca96254b7c2@ec2-54-174-51-35.compute-1.amazonaws.com/job/portal/buildStatus/icon)](http://wingsbuild:0db28aa0f4fc0685df9a216fc7af0ca96254b7c2@ec2-54-174-51-35.compute-1.amazonaws.com/job/portal/)

Wings Project Setup instructions :
1) Maven download : https://maven.apache.org/download.cgi 

2) Project Checkout from repository:  https://github.com/wings-software/wings

3) Set up Project:
3a) Eclipse project - Run "mvn eclipse:eclipse -DdownloadSources=true" command in the project checkout directory.

4) Import the project in the eclipse.
4a) Eclipse Download: http://www.eclipse.org/downloads/
4b) Under Window/Preferences select Java/Code Style/Formatter. Import the settings file codeStyle/eclipse-java-google-style.xml by selecting Import.
4c) Import codeStyle/eclipse-java-google-style.xml in eclipse.

5) Intellij Setup:
5a) Import portal in intellij as maven project.
5b) Import codeStyle/intellij-java-google-style.xml in intellij Settings/Editor/CodeStyle/Manage.

6) To build project along with ui:
6a) Install npm: https://github.com/nodesource/distributions.
6b) cd to wings directory and checkout UI project from repository: https://github.com/wings-software/wingsui
6c) Run "mvn package -DbuildUI=true"

7) to apply database migrations run following command in dbmigrations folder:
   "mvn clean compile exec:java"
