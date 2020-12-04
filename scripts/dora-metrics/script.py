import datetime

import requests
import time
import csv

CSV_COL_APPLICATION_NAME = "Application Name"
CSV_COL_ENTITY_TYPE = "Entity Type"
CSV_COL_PIPELINE_NAME = "Pipeline Name"
CSV_COL_WORKFLOW_NAME = "Workflow Name"
CSV_COL_STATUS = "Status"
CSV_COL_TAGS = "Tag(s)"
CSV_COL_START_DATE = "Start Date"
CSV_COL_START_TIME = "Start Time"
CSV_COL_END_DATE = "End Date"
CSV_COL_END_TIME = "End Time"
CSV_COL_TRIGGER_TYPE = "Triggered Type"
CSV_COL_TRIGGER_DETAILS = "Trigger Details (User email or trigger name)"
CSV_COL_ENVIRONMENT = "Environment(s)"
CSV_COL_SERVICE = "Service(s)"
CSV_COL_ARTIFACT_SOURCE = "Artifact Source(s)"
CSV_COL_ARTIFACT_BUILD = "Artifiact Build No(s)"
CSV_HEADERS = [CSV_COL_APPLICATION_NAME, CSV_COL_ENTITY_TYPE, CSV_COL_PIPELINE_NAME, CSV_COL_WORKFLOW_NAME,
               CSV_COL_STATUS, CSV_COL_TAGS, CSV_COL_START_DATE, CSV_COL_START_TIME, CSV_COL_END_DATE, CSV_COL_END_TIME,
               CSV_COL_TRIGGER_TYPE, CSV_COL_TRIGGER_DETAILS, CSV_COL_ENVIRONMENT, CSV_COL_SERVICE,
               CSV_COL_ARTIFACT_SOURCE, CSV_COL_ARTIFACT_BUILD]

CAUSE_EXECUTED_BY_USER = "ExecutedByUser"
CAUSE_EXECUTED_BY_API_KEY = "ExecutedByAPIKey"
CAUSE_EXECUTED_BY_TRIGGER = "ExecutedByTrigger"
CAUSE_EXECUTED_ALONG_PIPELINE = "ExecutedAlongPipeline"

ENTITY_PIPELINE_EXECUTION = "PipelineExecution"
ENTITY_WORKFLOW_EXECUTION = "WorkflowExecution"

DEFAULT_API_KEY = "cHg3eGRfQkZSQ2ktcGZXUFlYVmp2dzo6ZEs3bkRPRVNGcG1XcWhuaEVRR2R3NjN6ZnVqYlFMZ1ZZT2pmNGEyb3dBMVdJQlBuNTVXclVVdEZjYWdCQkdJd0xwMFdPa3RxUml4VTRwRWw="
CLIENT_ACCOUNT_ID = "px7xd_BFRCi-pfWPYXVjvw"

HEADER_X_API_KEY = "x-api-key"
PAYLOAD_PARAM_QUERY = "query"
GRAPHQL_QUERY = "{ executions(limit: 100, offset: $OFFSET, $FILTER) { pageInfo { hasMore limit offset total } nodes { id application { name } status tags { name value } __typename cause { ... on ExecutedByUser { user { email } } ... on ExecutedByAPIKey { apiKey { name } } ... on ExecutedByTrigger { trigger { name } } ... on ExecutedAlongPipeline { execution { pipeline { name } } } } startedAt endedAt ... on PipelineExecution { pipeline { name } memberExecutions { nodes { ... on WorkflowExecution { workflow { name } status tags { name value } startedAt endedAt artifacts { buildNo artifactSource { name } } cause { ... on ExecutedByUser { user { email } } ... on ExecutedByAPIKey { apiKey { name } } ... on ExecutedByTrigger { trigger { name } } ... on ExecutedAlongPipeline { execution { pipeline { name } } } } outcomes { nodes { ... on DeploymentOutcome { environment { name type } service { name deploymentType } } } } } } } } ... on WorkflowExecution { workflow { name } artifacts { buildNo artifactSource { name } } outcomes { nodes { ... on DeploymentOutcome { environment { name type } service { name deploymentType } } } } } } } } "
GRAPHQL_PIPELINE_FILTER = '{pipeline:{operator:EQUALS,values:["$PIPELINE_ID"]}}'
GRAPHQL_WORKFLOW_FILTER = '{workflow:{operator:EQUALS,values:["$WORKFLOW_ID"]}}'
GRAPHQL_BEFORE_DATE_FILTER = '{startTime: {operator: BEFORE, value: $VALUE}}'
GRAPHQL_AFTER_DATE_FILTER = '{startTime: {operator: AFTER, value: $VALUE}}'
GRAPHQL_FILTER = 'filters:[$GRAPHQL_PIPELINE_FILTER, $GRAPHQL_WORKFLOW_FILTER, $GRAPHQL_BEFORE_DATE_FILTER, $GRAPHQL_AFTER_DATE_FILTER]'

FILTER_PLACE_HOLDER = "$FILTER"
OFFSET_PLACE_HOLDER = "$OFFSET"
VALUE_PLACE_HOLDER = "$VALUE"
PIPELINE_FILTER_PLACE_HOLDER = "$GRAPHQL_PIPELINE_FILTER"
WORKFLOW_FILTER_PLACE_HOLDER = "$GRAPHQL_WORKFLOW_FILTER"
BEFORE_DATE_FILTER_PLACE_HOLDER = "$GRAPHQL_BEFORE_DATE_FILTER"
AFTER_DATE_FILTER_PLACE_HOLDER = "$GRAPHQL_AFTER_DATE_FILTER"
WORKFLOW_ID_FILTER_PLACE_HOLDER = "$WORKFLOW_ID"
PIPELINE_ID_FILTER_PLACE_HOLDER = "$PIPELINE_ID"

REQUEST_URL = "https://qa.harness.io/gateway/api/graphql?accountId="
REQUEST_HEADERS = {
    "content-type": "application/json",
}
REQUEST_RETRY_COUNT = 3

SECONDS_IN_SINGLE_YEAR = 31536000
SECONDS_IN_SINGLE_DAY = 86400
MISSING_VALUE_PLACE_HOLDER = "<< DELETED >>"
FILE_STATUS_NEW = "NEW"
FILE_STATUS_EXISTING = "EXISTING"
TEMP_CSV_FILE_NAME = "temp.csv"

#### helper methods #####

def print_list(data):
    for i in range(len(data)):
        print(data[i])


def get_past_time_from_epoch(curr_epoch_in_seconds, years, months, days):
    # assuming 30 days in each month
    return curr_epoch_in_seconds - (
                years * SECONDS_IN_SINGLE_YEAR + months * 30 * SECONDS_IN_SINGLE_DAY + days * SECONDS_IN_SINGLE_DAY)


def join_list_items(itemList, separator):
    return separator.join(itemList)

#########################

def custom_workflow_parser_helper(items_set):
    # if len > 1, that means it contains valid values as well
    # just keep valid values and remove MISSING VALUE
    if len(items_set) > 1:
        items_set.discard(MISSING_VALUE_PLACE_HOLDER)

    # return list of items
    return list(items_set)


def construct_graphql_filter(entity_type, entity_id, start_time_epoch, end_time_epoch):
    graphql_filter = GRAPHQL_FILTER

    if entity_type == ENTITY_PIPELINE_EXECUTION:
        graphql_filter = graphql_filter.replace(PIPELINE_FILTER_PLACE_HOLDER, GRAPHQL_PIPELINE_FILTER)
        graphql_filter = graphql_filter.replace(PIPELINE_ID_FILTER_PLACE_HOLDER, entity_id)
    else :
        graphql_filter = graphql_filter.replace(PIPELINE_FILTER_PLACE_HOLDER, "")

    if entity_type == ENTITY_WORKFLOW_EXECUTION:
        graphql_filter = graphql_filter.replace(WORKFLOW_FILTER_PLACE_HOLDER, GRAPHQL_WORKFLOW_FILTER)
        graphql_filter = graphql_filter.replace(WORKFLOW_ID_FILTER_PLACE_HOLDER, entity_id)
    else:
        graphql_filter = graphql_filter.replace(WORKFLOW_FILTER_PLACE_HOLDER, "")

    if start_time_epoch is None:
        graphql_filter = graphql_filter.replace(AFTER_DATE_FILTER_PLACE_HOLDER, "")
    else:
        graphql_filter = graphql_filter.replace(AFTER_DATE_FILTER_PLACE_HOLDER, GRAPHQL_AFTER_DATE_FILTER)
        graphql_filter = graphql_filter.replace(VALUE_PLACE_HOLDER, str(start_time_epoch*1000))

    if end_time_epoch is None:
        graphql_filter = graphql_filter.replace(BEFORE_DATE_FILTER_PLACE_HOLDER, "")
    else:
        graphql_filter = graphql_filter.replace(BEFORE_DATE_FILTER_PLACE_HOLDER, GRAPHQL_BEFORE_DATE_FILTER)
        graphql_filter = graphql_filter.replace(VALUE_PLACE_HOLDER, str(end_time_epoch*1000))

    return graphql_filter


def construct_graphql_query(entity_type, entity_id, offset, start_time_epoch, end_time_epoch):
    graphql_query = GRAPHQL_QUERY
    graphql_query = graphql_query.replace(OFFSET_PLACE_HOLDER, offset)

    graphql_query = graphql_query.replace(FILTER_PLACE_HOLDER, construct_graphql_filter(entity_type, entity_id, start_time_epoch, end_time_epoch))

    print(graphql_query)
    return graphql_query


def get_all_deployments(api_key, account_id, offset, entity_type, entity_id, start_time_epoch, end_time_epoch):
    retry_count = REQUEST_RETRY_COUNT

    request_url = REQUEST_URL + account_id

    request_headers = REQUEST_HEADERS
    request_headers[HEADER_X_API_KEY] = api_key

    request_payload = {
        PAYLOAD_PARAM_QUERY: construct_graphql_query(entity_type, entity_id, offset, start_time_epoch, end_time_epoch)
    }

    # retry in case of failures (non-200 HTTP responses)
    while retry_count > 0:
        response = requests.post(request_url, json=request_payload, headers=request_headers)
        if response.status_code == 200:
            return response.json()

        print("RETRYING")
        # reduce retry count on each repetition
        retry_count -= 1

    raise Exception("Unable to get valid HTTP 200 response")


def get_pipeline_executions(query_result):
    return query_result['data']['executions']['nodes']


def get_page_details(query_result):
    print(query_result)
    page_info = query_result['data']['executions']['pageInfo']
    return page_info['hasMore'], page_info['limit']


def get_field(execution, fieldName):
    try:
        if fieldName == CSV_COL_APPLICATION_NAME:
            return execution['application']['name']

        if fieldName == CSV_COL_ENTITY_TYPE:

            return execution['__typename']

        if fieldName == CSV_COL_STATUS:
            return execution['status']

        if fieldName == CSV_COL_TAGS:
            formattedTagsList = []
            tagsList = execution['tags']

            for tag in tagsList:
                formattedTagsList.append(tag['name'] + ":" + tag['value'])

            return formattedTagsList

        if fieldName == CSV_COL_START_DATE:
            return time.strftime('%d-%b-%Y', time.localtime(execution['startedAt'] / 1000))

        if fieldName == CSV_COL_START_TIME:
            return time.strftime('%H:%M:%S', time.localtime(execution['startedAt'] / 1000))

        if fieldName == CSV_COL_END_DATE and execution['endedAt'] is not None:
            return time.strftime('%d-%b-%Y', time.localtime(execution['endedAt'] / 1000))

        if fieldName == CSV_COL_END_TIME and execution['endedAt'] is not None:
            return time.strftime('%H:%M:%S', time.localtime(execution['endedAt'] / 1000))

        if fieldName == CSV_COL_TRIGGER_TYPE:
            if 'user' in execution['cause']:
                return CAUSE_EXECUTED_BY_USER
            if 'apiKey' in execution['cause']:
                return CAUSE_EXECUTED_BY_API_KEY
            if 'trigger' in execution['cause']:
                return CAUSE_EXECUTED_BY_TRIGGER
            if 'execution' in execution['cause']:
                return CAUSE_EXECUTED_ALONG_PIPELINE

        if fieldName == CSV_COL_WORKFLOW_NAME:
            if execution['workflow'] is None or 'name' not in execution['workflow']:
                return MISSING_VALUE_PLACE_HOLDER
            else:
                return execution['workflow']['name']
    except:
        return MISSING_VALUE_PLACE_HOLDER

    return MISSING_VALUE_PLACE_HOLDER


def get_entity_name(execution, entity_type):
    if entity_type == ENTITY_PIPELINE_EXECUTION and execution['pipeline'] is not None:
        if 'name' in execution['pipeline']:
            return execution['pipeline']['name'], ""
        else:
            return MISSING_VALUE_PLACE_HOLDER, ""

    if entity_type == ENTITY_WORKFLOW_EXECUTION and execution['workflow'] is not None:
        if 'name' in execution['workflow']:
            return "", execution['workflow']['name']
        else:
            return "", MISSING_VALUE_PLACE_HOLDER

    return "", ""


def get_trigger_details(execution, trigger_type):
    if trigger_type == CAUSE_EXECUTED_BY_USER and execution['cause'] is not None:
        if 'user' in execution['cause'] and execution['cause']['user'] is not None:
            if 'email' in execution['cause']['user']:
                return execution['cause']['user']['email']

    if trigger_type == CAUSE_EXECUTED_BY_API_KEY and execution['cause'] is not None:
        if 'apiKey' in execution['cause'] and execution['cause']['apiKey'] is not None:
            if 'name' in execution['cause']['apiKey']:
                return execution['cause']['apiKey']['name']

    if trigger_type == CAUSE_EXECUTED_BY_TRIGGER and execution['cause'] is not None:
        if 'trigger' in execution['cause'] and execution['cause']['trigger'] is not None:
            if 'name' in execution['cause']['trigger']:
                return execution['cause']['trigger']['name']

    if trigger_type == CAUSE_EXECUTED_ALONG_PIPELINE and execution['cause'] is not None:
        if 'execution' in execution['cause'] and execution['cause']['execution'] is not None:
            if 'pipeline' in execution['cause']['execution'] and execution['cause']['execution']['pipeline'] is not None:
                if 'name' in execution['cause']['execution']['pipeline']:
                    return execution['cause']['execution']['pipeline']['name']

    return MISSING_VALUE_PLACE_HOLDER


def get_workflow_details(workflows):
    # env = []
    # services = []
    # artifacts = []
    # artifacts_sources = []
    workflow_details_list = []

    env = set()
    services = set()
    artifacts = set()
    artifacts_sources = set()

    for workflow in workflows:
        workflow_details = {}
        workflow_details[CSV_COL_WORKFLOW_NAME] = get_field(workflow, CSV_COL_WORKFLOW_NAME)
        workflow_details[CSV_COL_STATUS] = get_field(workflow, CSV_COL_STATUS)
        workflow_details[CSV_COL_TAGS] = get_field(workflow, CSV_COL_TAGS)
        workflow_details[CSV_COL_START_DATE] = get_field(workflow, CSV_COL_START_DATE)
        workflow_details[CSV_COL_START_TIME] = get_field(workflow, CSV_COL_START_TIME)
        workflow_details[CSV_COL_END_DATE] = get_field(workflow, CSV_COL_END_DATE)
        workflow_details[CSV_COL_END_TIME] = get_field(workflow, CSV_COL_END_TIME)
        workflow_details[CSV_COL_TRIGGER_TYPE] = get_field(workflow, CSV_COL_TRIGGER_TYPE)
        workflow_details[CSV_COL_TRIGGER_DETAILS] = get_trigger_details(workflow,
                                                                        workflow_details[CSV_COL_TRIGGER_TYPE])

        service = None
        environment = None
        artifact_build = None
        artifact_source = None

        if 'outcomes' in workflow and workflow['outcomes'] is not None:
            if 'nodes' in workflow['outcomes']:
                outcomes = workflow['outcomes']['nodes']
                if outcomes is not None:
                    # init empty string to identify genuine cases that exist without env or service
                    environment = ""
                    service = ""
                    for outcome in outcomes:
                        # if valid outcome exists, then valid values for env or service is expected
                        # so reset env and service to NONE
                        # if its not set now, it denotes to cases with missing data
                        environment = None
                        service = None
                        if 'environment' in outcome and outcome['environment'] is not None:
                            if 'name' in outcome['environment']:
                                environment = outcome['environment']['name']
                                # env.append(outcome['environment']['name'])

                        if 'service' in outcome and outcome['service'] is not None:
                            if 'name' in outcome['service']:
                                service = outcome['service']['name']
                                # services.append(outcome['service']['name'])

        if 'artifacts' in workflow and workflow['artifacts'] is not None:
            artifact_build = ""
            artifact_source = ""
            for artifact in workflow['artifacts']:
                artifact_build = None
                artifact_source = None
                if 'buildNo' in artifact:
                    artifact_build = artifact['buildNo']
                    # artifacts.append(artifact['buildNo'])
                if 'artifactSource' in artifact and artifact['artifactSource'] is not None:
                    if 'name' in artifact['artifactSource']:
                        artifact_source = artifact['artifactSource']['name']
                        # artifacts_sources.append(artifact['artifactSource']['name'])

        if service is None:
            workflow_details[CSV_COL_SERVICE] = MISSING_VALUE_PLACE_HOLDER
        else:
            workflow_details[CSV_COL_SERVICE] = service
        if environment is None:
            workflow_details[CSV_COL_ENVIRONMENT] = MISSING_VALUE_PLACE_HOLDER
        else:
            workflow_details[CSV_COL_ENVIRONMENT] = environment
        if artifact_build is None:
            workflow_details[CSV_COL_ARTIFACT_BUILD] = MISSING_VALUE_PLACE_HOLDER
        else:
            workflow_details[CSV_COL_ARTIFACT_BUILD] = artifact_build
        if artifact_source is None:
            workflow_details[CSV_COL_ARTIFACT_SOURCE] = MISSING_VALUE_PLACE_HOLDER
        else:
            workflow_details[CSV_COL_ARTIFACT_SOURCE] = artifact_source

        services.add(workflow_details[CSV_COL_SERVICE])
        env.add(workflow_details[CSV_COL_ENVIRONMENT])
        artifacts.add(workflow_details[CSV_COL_ARTIFACT_BUILD])
        artifacts_sources.add(workflow_details[CSV_COL_ARTIFACT_SOURCE])

        workflow_details_list.append(workflow_details)

    # TODO
    # Need to try and handle env, services, artifacts etc from the workflow_details_list as separate method
    # rather than just creating multiple copies at same time
    # return env, services, artifacts, artifacts_sources, workflow_details_list

    ###
    # parse services, env, artifacts and artifacts sources
    # convert it to list with contains either only MISSING VALUE HOLDER or only valid values
    # its required to show summarized pipeline view
    ###

    return custom_workflow_parser_helper(env), custom_workflow_parser_helper(services), custom_workflow_parser_helper(artifacts), custom_workflow_parser_helper(artifacts_sources), workflow_details_list

#
# def is_interval_threshold_breached(execution_start_date, start_time_interval_epoch):
#     # compare epoch time for interval start time and current execution start date
#     curr_execution_time_epoch = datetime.datetime.strptime(execution_start_date, '%d-%b-%Y').strftime('%s')
#     if int(curr_execution_time_epoch) < int(start_time_interval_epoch):
#         return True
#
#     return False
#
#
# def is_execution_within_search_interval(execution_start_date, start_time_interval_epoch, end_time_interval_epoch):
#     # check if current execution start date lies within interval start and end epoch time
#     curr_execution_time_epoch = datetime.datetime.strptime(execution_start_date, '%d-%b-%Y').strftime('%s')
#     if int(curr_execution_time_epoch) > int(end_time_interval_epoch):
#         return False
#     if int(curr_execution_time_epoch) < int(start_time_interval_epoch):
#         return False
#
#     return True


def prepare_csv_row(entry_details):
    return [entry_details.get(CSV_COL_APPLICATION_NAME, ""),
            entry_details.get(CSV_COL_ENTITY_TYPE, ""),
            entry_details.get(CSV_COL_PIPELINE_NAME, ""),
            entry_details.get(CSV_COL_WORKFLOW_NAME, ""),
            entry_details.get(CSV_COL_STATUS, ""),
            entry_details.get(CSV_COL_TAGS, ""),
            entry_details.get(CSV_COL_START_DATE, ""),
            entry_details.get(CSV_COL_START_TIME, ""),
            entry_details.get(CSV_COL_END_DATE, ""),
            entry_details.get(CSV_COL_END_TIME, ""),
            entry_details.get(CSV_COL_TRIGGER_TYPE, ""),
            entry_details.get(CSV_COL_TRIGGER_DETAILS, ""),
            entry_details.get(CSV_COL_ENVIRONMENT, ""),
            entry_details.get(CSV_COL_SERVICE, ""),
            entry_details.get(CSV_COL_ARTIFACT_SOURCE, ""),
            entry_details.get(CSV_COL_ARTIFACT_BUILD, "")]


def create_csv_data(executions):
    data_rows = []

    for execution in executions:
        print(execution)

        # pipeline specific details
        application_name = get_field(execution, CSV_COL_APPLICATION_NAME)
        entity_type = get_field(execution, CSV_COL_ENTITY_TYPE)
        pipeline_name, workflow_name = get_entity_name(execution, entity_type)
        status = get_field(execution, CSV_COL_STATUS)
        tags = get_field(execution, CSV_COL_TAGS)
        start_date = get_field(execution, CSV_COL_START_DATE)
        start_time = get_field(execution, CSV_COL_START_TIME)
        end_date = get_field(execution, CSV_COL_END_DATE)
        end_time = get_field(execution, CSV_COL_END_TIME)
        trigger_type = get_field(execution, CSV_COL_TRIGGER_TYPE)
        trigger_name = get_trigger_details(execution, trigger_type)
        #
        # # if current execution is older than the interval start time, we skip rest of the executions and return
        # if is_interval_threshold_breached(start_date, start_time_interval_epoch) is True:
        #     return data_rows, True
        # # if current execution is not within search interval, skip it
        # if is_execution_within_search_interval(start_date, start_time_interval_epoch, end_time_interval_epoch) is False:
        #     continue

        # fetch env, services and artifacts details from workflow/s
        workflows = []

        if entity_type == ENTITY_PIPELINE_EXECUTION:
            workflows = execution['memberExecutions']['nodes']
        else:
            workflows.append(execution)

        env, services, artifacts, artifacts_sources, sub_execution_details = get_workflow_details(workflows)

        main_execution_details = {
            CSV_COL_APPLICATION_NAME: application_name,
            CSV_COL_ENTITY_TYPE: entity_type,
            CSV_COL_PIPELINE_NAME: pipeline_name,
            CSV_COL_WORKFLOW_NAME: workflow_name,
            CSV_COL_STATUS: status,
            CSV_COL_TAGS: join_list_items(tags, ","),
            CSV_COL_START_DATE: start_date,
            CSV_COL_START_TIME: start_time,
            CSV_COL_END_DATE: end_date,
            CSV_COL_END_TIME: end_time,
            CSV_COL_TRIGGER_TYPE: trigger_type,
            CSV_COL_TRIGGER_DETAILS: trigger_name,
            CSV_COL_ENVIRONMENT: join_list_items(env, ","),
            CSV_COL_SERVICE: join_list_items(services, ","),
            CSV_COL_ARTIFACT_SOURCE: join_list_items(artifacts_sources, ","),
            CSV_COL_ARTIFACT_BUILD: join_list_items(artifacts, ",")
        }

        data_rows.append(prepare_csv_row(main_execution_details))

        if entity_type == ENTITY_PIPELINE_EXECUTION:
            # append workflow executions as separate entries in the csv
            for sub_execution in sub_execution_details:
                # add pipeline name to the sub execution
                sub_execution[CSV_COL_PIPELINE_NAME] = pipeline_name

                data_rows.append(prepare_csv_row(sub_execution))

        # entry = [application_name, entity_type, pipeline_name, workflow_name, status, ','.join(tags), start_date,
        #          start_time, end_date, end_time, trigger_type, trigger_name, ','.join(env), ','.join(services),
        #          ','.join(artifacts_sources), ','.join(artifacts)]
        # data_rows.append(entry)

    return data_rows, False


def init_csv_file(filename):
    with open(filename, 'w') as csvfile:
        # create new csv file and put headers in it
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the fields
        csvwriter.writerow(CSV_HEADERS)


def create_new_csv(filename):
    with open(filename, 'w') as csvfile:
        pass


def append_to_csv_file(filename, data_rows):
    with open(filename, 'a') as csvfile:
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the data rows
        csvwriter.writerows(data_rows)


def copy_csv_file(source_file, dest_file):
    with open(dest_file, 'a+') as w:
        writer = csv.writer(w)
        with open(source_file, 'r') as f:
            reader = csv.reader(f)
            for row in reader:
                writer.writerow(row)


def compile_data(api_key, start_time_interval_epoch, end_time_interval_epoch, entity_type, entity_id, filename, file_status):
    if api_key == "":
        api_key = DEFAULT_API_KEY

    account_id = CLIENT_ACCOUNT_ID

    if entity_type == 'w':
        entity_type = ENTITY_WORKFLOW_EXECUTION
    if entity_type == 'p':
        entity_type = ENTITY_PIPELINE_EXECUTION

    filename += ".csv"
    if file_status == 'n':
        file_status = FILE_STATUS_NEW
        init_csv_file(filename)
    else:
        file_status = FILE_STATUS_EXISTING

    if start_time_interval_epoch == "":
        # default interval is last 1 month from now, so calculate interval start time accordingly
        start_time_interval_epoch = get_past_time_from_epoch(int(time.time()), 0, 1, 0)
    if end_time_interval_epoch == "":
        end_time_interval_epoch = int(time.time())

    curr_offset = 0
    repeat = True

    # create blank temp file
    create_new_csv(TEMP_CSV_FILE_NAME)

    while repeat is True:
        # fetch all deployments via graphql query in pagination manner
        deployments_result = get_all_deployments(api_key, account_id, str(curr_offset), entity_type, entity_id, start_time_interval_epoch, end_time_interval_epoch)

        # get updated offset
        repeat, limit = get_page_details(deployments_result)

        # parse deployments
        executions = get_pipeline_executions(deployments_result)

        # parse and create data rows out of the executions
        data_rows, is_threshold_breached = create_csv_data(executions)

        # append all data rows to the temp csv
        append_to_csv_file(TEMP_CSV_FILE_NAME, data_rows)

        if is_threshold_breached:
            repeat = False
        else:
            curr_offset = curr_offset + limit

    # copy all data from current temp file to the original file
    copy_csv_file(TEMP_CSV_FILE_NAME, filename)

    print("--XX--COMPLETED--XX--")