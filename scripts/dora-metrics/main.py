import script
from datetime import date, datetime
import time


def get_date_obj(date_input):
    try:
        date_time_obj = datetime.strptime(date_input, '%m/%d/%Y')
        return date_time_obj
    except:
        return None


if __name__ == "__main__":
    api_key = input("Enter API-KEY : ")

    while True:
        file_status = input("Enter E for using existing file (leave blank for New file) : ")

        if file_status == 'e' or file_status == 'E' or file_status == "":
            if file_status == "":
                file_status = 'n'
            file_status = file_status.lower()
            break
        print("ERROR : invalid input, please try again")

    while True:
        filename = input("Enter name of file : ")
        if filename != "":
            break
        print("ERROR : invalid input, please try again")

    entity_id = ""
    while True:
        entity_type = input("Fetch deployments - Enter W for workflow or P for pipeline (leave blank for all) : ")
        if entity_type == "":
            # fetch all deployments
            break

        entity_type = entity_type.lower()
        if entity_type == 'w' or entity_type == 'p':
            entity_id = input("Enter job id : ")

    while True:
        start_time_input = input("Enter start time of search interval (MM/DD/YYYY) : ")
        start_time_obj = get_date_obj(start_time_input)
        if start_time_obj is not None:
            start_time_epoch = int(start_time_obj.timestamp())
            break
        print("ERROR : Date invalid, please try again")

    while True:
        end_time_input = input("Enter end time of search interval (MM/DD/YYYY) (leave blank to search till now) : ")
        if end_time_input == "":
            end_time_epoch = int(time.time())
            break
        end_time_obj = get_date_obj(end_time_input)
        if end_time_obj is not None:
            end_time_epoch = int(end_time_obj.timestamp())
            break
        print("ERROR : Date invalid, please try again")

    script.compile_data(api_key, start_time_epoch, end_time_epoch, entity_type, entity_id, filename, file_status)
