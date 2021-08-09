1. Checkout the Specific BE and UI branches. 
2. Build BE : mvn clean install -DskipTests and UI: yarn build
3. Update version.properties with details 
4. Update installer.properties using Portal and UI code path 
5. Call prepare_installer.sh, it internally copies all required data and generate the installer 
6. Once installer copied to rhel box, Do below step to to add LE
    > Extract the installer
    > Get the LE version from LE Team, Download and extract it
    > Create le folder inside installer and copy the splunk_pyml folder from LE build to le folder
7. Create installer from extracted folder using tar command and upload the tar file to GCP.     
