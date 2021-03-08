# Deployment Executions Metrics Report Generator

## FEATURES
* Search based on given workflow id/pipeline id or all executions
* Search based on time filters
* Search by tags
* Scope for creating new output file or append metrics to existing file
* Debug and error logs support
* Output generated in form of csv file

## HOW TO USE
1. Use python main program file `workflow_driver.py` to generate the report.
2. Install dependencies as below :
    ```
   brew install python3
   pip3 install requests
   ```
2. Give execution permissions to the program file by using following command :
    ```commandline
    chmod +x workflow_driver.py
   ```
3. Trigger the script using following command with proper input params :
    ```commandline
    python3 workflow_driver.py <APP_DOMAIN> <API_KEY> <ACCOUNT_ID> <FILE_OPERATION_ID> <OUTPUT_FILE_DIRECTORY_PATH> <OUTPUT_FILE_NAME> <META_FILE_DIRECTORY_PATH> <ENTITY_TYPE> <ENTITY_ID> <SEARCH_INTERVAL_START_TIME> <SEARCH_INTERVAL_END_TIME> <TAG_ENTITY_TYPE> <TAG_PAIR_LIST>
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
   |ENTITY_TYPE|Identifier to denote whether to search Workflow or Pipeline or all executions|WORKFLOW / PIPELINE / null (for All Executions)|
   |ENTITY_ID|- Workflow/Pipeline id in case of WORKFLOW/PIPELINE entity type<br>- null in case of all executions|<-valid id->|
   |SEARCH_INTERVAL_START_TIME|Start time of the search interval in format MM/DD/YYYY HH:MM:SS (24-Hr format) in local timezone|09/21/2020 00:00:00|
   |SEARCH_INTERVAL_END_TIME|End time of the search interval in format MM/DD/YYYY HH:MM:SS (24-Hr format) in local timezone|09/21/2020 00:00:00|
   |TAG_ENTITY_TYPE|- Identifier to denote type of tags to search<br>- null to ignore tags|APPLICATION / DEPLOYMENT / ENVIRONMENT / SERVICE / null|
   |TAG_PAIR_LIST|- List of tag name and value in defined format<br>- tags ignored if TAG_ENTITY_TYPE is null|[tag_name=tag_value,commitId=122,env=Prod]|
   
   Sample Query : 
   ```
   python3 workflow_driver.py "https://qa.harness.io/gateway" "cHg3eGRfQkZSQ2ktcGZXUFlYVmp2dzo6ZEs3bkRPRVNGcG1XcWhuaEVRR2R3NjN6ZnVqYlFMZ1ZZT2pmNGEyb3dBMVdJQlBuNTVXclVVdEZjYWdCQkdJd0xwMFdPa3RxUml4VTRwRWw=" "px7xd_BFRCi-pfWPYXVjvw" "NEW" "/Users/John" "TEST" "/Users/John/Documents/meta" "null" "null" "12/02/2020 00:00:00" "12/05/2020 00:00:00" "null" "null"
   ```
