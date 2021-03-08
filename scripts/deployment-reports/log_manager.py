import helper
import json

DEBUG_LOG_KEY_INPUT_PARAMS = "input_params"
DEBUG_LOG_KEY_API_REQUEST = "api_request"
DEBUG_LOG_KEY_API_RESPONSE = "api_response"
DEBUG_LOG_KEY_LAST_EXECUTION_RECORD = "last_execution_record"

ERROR_LOG_API_FAILURE = "api_errors"
ERROR_LOG_EXCEPTION = "exception"

LOG_FORMAT_TIMESTAMP_KEY = "timestamp"
LOG_FORMAT_MESSAGE_KEY = "message"

DEBUG_LOG = {
    DEBUG_LOG_KEY_INPUT_PARAMS: {},
    DEBUG_LOG_KEY_API_REQUEST: {},
    DEBUG_LOG_KEY_API_RESPONSE: {},
    DEBUG_LOG_KEY_LAST_EXECUTION_RECORD: {},
}

ERROR_LOG = {
    ERROR_LOG_API_FAILURE: [],
    ERROR_LOG_EXCEPTION: {}
}


###### GETTERS ######

def get_debug_log_api_request():
    return DEBUG_LOG[DEBUG_LOG_KEY_API_REQUEST]


def get_debug_log_api_response():
    return DEBUG_LOG[DEBUG_LOG_KEY_API_RESPONSE]

#####################


def construct_log(val):
    if val is None:
        val = ""

    log_format = {
        LOG_FORMAT_TIMESTAMP_KEY: helper.get_current_time_local_version_str(),
        LOG_FORMAT_MESSAGE_KEY: str(val)
    }

    return log_format


def log_input_params(input_params):
    DEBUG_LOG[DEBUG_LOG_KEY_INPUT_PARAMS] = construct_log(input_params)


def log_request(uri, headers, payload):
    request_details = {
        "uri": uri,
        "headers": headers,
        "payload": payload
    }
    DEBUG_LOG[DEBUG_LOG_KEY_API_REQUEST] = construct_log(request_details)


def log_response(http_status, payload):
    response_details = {
        "http_status": http_status,
        "payload": payload
    }
    DEBUG_LOG[DEBUG_LOG_KEY_API_RESPONSE] = construct_log(response_details)


def log_last_execution(execution):
    DEBUG_LOG[DEBUG_LOG_KEY_LAST_EXECUTION_RECORD] = construct_log(execution)


def log_api_error():
    # if there's any api error, just put last recorded api request and response into error log
    api_error = {
        "request": get_debug_log_api_request(),
        "response": get_debug_log_api_response()
    }
    ERROR_LOG[ERROR_LOG_API_FAILURE].append(construct_log(api_error))


def log_exception(e):
    ERROR_LOG[ERROR_LOG_EXCEPTION] = construct_log(e)


def log_console_error(error):
    print("\n[ERROR] : ", str(error))


def log_console_message(message):
    print("\n[INFO] : ", str(message))


def get_debug_log():
    return json.dumps(DEBUG_LOG, indent=4)


def get_error_log():
    return json.dumps(ERROR_LOG, indent=4)
