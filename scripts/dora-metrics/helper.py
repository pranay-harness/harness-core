import datetime
import time
import os
import sys

DATE_FORMAT_MM_DD_YYYY = "%m/%d/%Y"
DATE_FORMAT_MM_DD_YYYY_HH_MM_SS = "%m/%d/%Y %H:%M:%S"
DATE_FORMAT_LOCAL_VERSION = "%c"
DATE_FORMAT_DD_MMM_YYYY = "%d-%b-%Y"
DATE_FORMAT_HH_MM_SS = "%H:%M:%S"
SECONDS_IN_SINGLE_YEAR = 31536000
SECONDS_IN_SINGLE_DAY = 86400


def get_date_obj_from_str(date_input_str, date_format):
    try:
        date_time_obj = datetime.datetime.strptime(date_input_str, date_format)
        return date_time_obj
    except:
        return None


def get_date_obj_from_epoch(epoch_in_seconds):
    epoch_date = datetime.datetime.fromtimestamp(epoch_in_seconds)
    return epoch_date


def get_local_timezone():
    local_timezone = datetime.datetime.now(datetime.timezone(datetime.timedelta(0))).astimezone().tzinfo
    return local_timezone


def convert_date_to_local_timezone(date_input_obj):
    return date_input_obj.astimezone(get_local_timezone())


def convert_date_to_timestamp(date_input_obj):
    try:
        return int(date_input_obj.timestamp())
    except:
        return None


def get_current_time_in_epoch_in_seconds():
    return int(time.time())


def get_current_local_date_obj():
    return convert_date_to_local_timezone(get_date_obj_from_epoch(get_current_time_in_epoch_in_seconds()))


def format_date(date_input_obj, date_format):
    return date_input_obj.strftime(date_format)


def get_current_time_local_version_str():
    return format_date(get_current_local_date_obj(), DATE_FORMAT_LOCAL_VERSION)


def get_current_working_directory_path_str():
    return os.path.dirname(sys.argv[0])


def get_absolute_file_path(filename):
    return os.path.join(get_current_working_directory_path_str(), filename)


def get_file_path(file_directory, filename):
    return os.path.join(file_directory, filename)


def is_directory_exists(path):
    if os.path.isdir(path):
        return True
    else:
        return False


def is_file_exists(filepath):
    if os.path.isfile(filepath):
        return True
    else:
        return False


def print_list(data):
    for i in range(len(data)):
        print(data[i])


def get_past_time_from_epoch(curr_epoch_in_seconds, years, months, days):
    # assuming 30 days in each month
    return curr_epoch_in_seconds - (
            years * SECONDS_IN_SINGLE_YEAR + months * 30 * SECONDS_IN_SINGLE_DAY + days * SECONDS_IN_SINGLE_DAY)


def join_list_items(item_list, separator):
    return separator.join(item_list)


def get_list_from_string(item_list, delimiter):
    try:
        if item_list[0] == '[':
            item_list = item_list[1:-1]
        if item_list == "":
            return []
        return item_list.split(delimiter)
    except Exception:
        return []