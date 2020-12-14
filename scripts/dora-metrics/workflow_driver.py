import sys
import script
import helper
import log_manager

# Sample command to run
# python3 workflow_driver.py "https://qa.harness.io/gateway" "cHg3eGRfQkZSQ2ktcGZXUFlYVmp2dzo6ZEs3bkRPRVNGcG1XcWhuaEVRR2R3NjN6ZnVqYlFMZ1ZZT2pmNGEyb3dBMVdJQlBuNTVXclVVdEZjYWdCQkdJd0xwMFdPa3RxUml4VTRwRWw=" "px7xd_BFRCi-pfWPYXVjvw" "NEW" "/Users/mohitgarg" "sample" "" "" "" "12/02/2020 00:00:00" "12/05/2020 00:00:00"

TOTAL_ARGS = 13

if __name__ == "__main__":
    args = sys.argv[1:]

    try:
        if len(args) < TOTAL_ARGS:
            raise Exception("Incomplete args, please try again")

        domain_name = args[0].strip()
        api_key = args[1].strip()
        account_id = args[2].strip()
        file_operation_id = args[3].strip()
        output_file_dir = args[4].strip()
        output_filename = args[5].strip()
        meta_file_dir = args[6].strip()
        entity_type = args[7].strip()
        entity_id = args[8].strip()
        start_time_date = args[9].strip()
        end_time_date = args[10].strip()
        tags_entity_type = args[11].strip()
        tags = args[12].strip()

        # put validation
        if file_operation_id != "APPEND" and file_operation_id != "NEW":
            raise Exception("Invalid file operation id")
        if file_operation_id == "APPEND":
            file_operation = script.FILE_OPERATION_APPEND
        else:
            file_operation = script.FILE_OPERATION_NEW

        if entity_type == "WORKFLOW" or entity_type == "PIPELINE" or entity_type == "":
            if entity_type == "":
                entity_type = script.ENTITY_ALL_EXECUTION
            else:
                if entity_type == "WORKFLOW":
                    entity_type = script.ENTITY_WORKFLOW_EXECUTION
                else:
                    entity_type = script.ENTITY_PIPELINE_EXECUTION
        else:
            raise Exception("Invalid entity type (Workflow/Pipeline/All)")

        if entity_type != script.ENTITY_ALL_EXECUTION:
            if entity_id == "":
                raise Exception("Empty entity id (Workflow id/Pipeline id)")

        start_time_obj = helper.get_date_obj_from_str(start_time_date, helper.DATE_FORMAT_MM_DD_YYYY_HH_MM_SS)
        if start_time_obj is not None:
            # convert date time object into local timezone
            start_time_obj = helper.convert_date_to_local_timezone(start_time_obj)
            # get epoch time corresponding to local timezone date
            start_time_epoch = helper.convert_date_to_timestamp(start_time_obj)
        else:
            raise Exception("Invalid start date")

        if end_time_date == "":
            end_time_epoch = helper.get_current_time_in_epoch_in_seconds()
            end_time_obj = helper.get_date_obj_from_epoch(end_time_epoch)
        else:
            end_time_obj = helper.get_date_obj_from_str(end_time_date, helper.DATE_FORMAT_MM_DD_YYYY_HH_MM_SS)
        if end_time_obj is not None:
            end_time_obj = helper.convert_date_to_local_timezone(end_time_obj)
            end_time_epoch = helper.convert_date_to_timestamp(end_time_obj)
        else:
            raise Exception("Invalid end date")

        tags_list = helper.get_list_from_string(tags, ",")
        tags_names_list = []
        tags_values_list = []
        for tag in tags_list:
            try:
                tags_names_list.append(helper.get_list_from_string(tag, "=")[0])
                tags_values_list.append(helper.get_list_from_string(tag, "=")[1])
            except Exception:
                raise Exception("Invalid tag format : " + tag)

        input_arguments = {
            script.ARGS_KEY_APP_DOMAIN_KEY: domain_name,
            script.ARGS_API_KEY: api_key,
            script.ARGS_ACCOUNT_ID_KEY: account_id,
            script.ARGS_FILE_OPERATION_KEY: file_operation,
            script.ARGS_OUTPUT_FILENAME_KEY: output_filename,
            script.ARGS_OUTPUT_FILE_DIR_KEY: output_file_dir,
            script.ARGS_META_FILE_DIR_KEY: meta_file_dir,
            script.ARGS_SEARCH_ENTITY_TYPE_KEY: entity_type,
            script.ARGS_SEARCH_ENTITY_ID_KEY: entity_id,
            script.ARGS_SEARCH_INTERVAL_START_TIME_EPOCH_KEY: start_time_epoch,
            script.ARGS_SEARCH_INTERVAL_END_TIME_EPOCH_KEY: end_time_epoch,
            script.ARGS_TAGS_ENTITY_TYPE_KEY: tags_entity_type,
            script.ARGS_TAGS_NAMES_LIST_KEY: tags_names_list,
            script.ARGS_TAGS_VALUES_LIST_KEY: tags_values_list
        }
        script.compile_data(input_arguments)
        sys.exit(200)
    except Exception as e:
        log_manager.log_console_error(e)
        sys.exit(400)
    finally:
        pass

