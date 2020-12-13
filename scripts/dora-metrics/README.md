# Dora Metrics Report Generator

## FEATURES
* Search based on given workflow id/pipeline id or all executions
* Search based on time filters
* Search by tags
* Scope for creating new output file or append metrics to existing file
* Debug and error logs support
* Output generated in form of csv file

## HOW TO USE
1. Use executable file `workflow_driver` to generate the report
2. Give execution permissions to the file by using following command :
    ```commandline
    chmod +x workflow_driver
   ```
3. Trigger the script using following command with proper input params :
    ```commandline
    ./workflow_driver <APP_DOMAIN> <API_KEY> <ACCOUNT_ID> <FILE_OPERATION_ID> <OUTPUT_FILE_DIRECTORY_PATH> <OUTPUT_FILE_NAME> <META_FILE_DIRECTORY_PATH> <ENTITY_TYPE> <ENTITY_ID> <SEARCH_INTERVAL_START_TIME> <SEARCH_INTERVAL_END_TIME> <TAG_ENTITY_TYPE> <TAG_NAMES_LIST> <TAG_VALUES_LIST>
   ```
   |Name|Description|Sample Value|
   |----|-----------|------------|
   |APP_DOMAIN|App domain required to fetch data|https://qa.harness.io/gateway|
   |API_KEY|User-group identifier|<-valid token->|
   |ACCOUNT_ID|Client Identifier|<-valid token->|
   |FILE_OPERATION_ID|Identifier to denote whether to create new file or append metrics to existing file|NEW / APPEND|
   |OUTPUT_FILE_DIRECTORY_PATH|- Existing file directory path in case of APPEND mode<br>- New file directory path in case of NEW mode|/Users/John/metrics/|
   |OUTPUT_FILE_NAME|- Existing CSV file name in case of APPEND mode<br>- New CSV file name in case of NEW mode|<-any valid file name->|
   |META_FILE_DIRECTORY_PATH|- Directory path to store debug and error log output files<br>- Default is the current working directory of the executable|/Users/John/metrics/meta_dir|
   |ENTITY_TYPE|Identifier to denote whether to search Workflow or Pipeline or all executions|WORKFLOW / PIPELINE / "" (empty string for All executions)|
   |ENTITY_ID|- Workflow/Pipeline id in case of WORKFLOW/PIPELINE entity type<br>- empty string in case of all executions|<-valid id->|
   |SEARCH_INTERVAL_START_TIME|Start time of the search interval in format MM/DD/YYYY HH:MM:SS (24-Hr format) in local timezone|09/21/2020 00:00:00|
   |SEARCH_INTERVAL_END_TIME|End time of the search interval in format MM/DD/YYYY HH:MM:SS (24-Hr format) in local timezone|09/21/2020 00:00:00|
   |TAG_ENTITY_TYPE|Identifier to denote type of tags to search|APPLICATION / DEPLOYMENT / ENVIRONMENT / SERVICE|
   |TAG_NAMES_LIST|List of comma separated names of each tag to be searched|[commitId,env]|
   |TAG_VALUES_LIST|List of comma seaparated values of each tag to be searched|[122,prod]|
   
   NOTES :
    * A single tag's name and value must be present at same index in the names and values list i.e. the order should be maintained
   
