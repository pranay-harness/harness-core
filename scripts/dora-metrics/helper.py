import datetime
import time

DATE_FORMAT_MM_DD_YYYY = "%m/%d/%Y"
SECONDS_IN_SINGLE_YEAR = 31536000
SECONDS_IN_SINGLE_DAY = 86400


def get_date_obj_from_str(date_input):
    try:
        date_time_obj = datetime.datetime.strptime(date_input, DATE_FORMAT_MM_DD_YYYY)
        return date_time_obj
    except:
        return None


def get_date_obj_from_epoch(epoch_in_seconds):
    epoch_date = datetime.datetime.fromtimestamp(epoch_in_seconds)
    return epoch_date


def get_local_timezone():
    local_timezone = datetime.datetime.now(datetime.timezone(datetime.timedelta(0))).astimezone().tzinfo
    return local_timezone


def convert_date_to_local_timezone(date_input):
    return date_input.astimezone(get_local_timezone())


def get_current_time_in_epoch_in_seconds():
    return int(time.time())


def print_list(data):
    for i in range(len(data)):
        print(data[i])


def get_past_time_from_epoch(curr_epoch_in_seconds, years, months, days):
    # assuming 30 days in each month
    return curr_epoch_in_seconds - (
            years * SECONDS_IN_SINGLE_YEAR + months * 30 * SECONDS_IN_SINGLE_DAY + days * SECONDS_IN_SINGLE_DAY)


def join_list_items(itemList, separator):
    return separator.join(itemList)