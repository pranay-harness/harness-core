import datetime
import os
import util
import re
import time

from google.cloud import bigquery
from google.cloud import storage

from util import create_dataset, if_tbl_exists, createTable, print_

"""
Step1) Ingest data from avro to Bigquery's clusterData table.
Step2) Run the SQL to load into unifiedTable in the same dataset.

event:
{
     'bucket': 'clusterdata-dev',
     'contentType': 'application/octet-stream',
     'crc32c': '+4CFsw==',
     'etag': 'CJX7js2X9uwCEAE=',
     'generation': '1604949714910613',
     'id': 'clusterdata-dev/nvsv7gjbtzya3cgsgxnocg/WhejVM7NTJe2fZ99Pdo2YA_billing_data_2020_NOVEMBER_1.avro/1604949714910613',
     'kind': 'storage#object',
     'md5Hash': 'SXp0LaXb6gusNS5+1F+MAA==',
     'mediaLink': 'https://www.googleapis.com/download/storage/v1/b/clusterdata-dev/o/nvsv7gjbtzya3cgsgxnocg%2FWhejVM7NTJe2fZ99Pdo2YA_billing_data_2020_NOVEMBER_1.avro?generation=1604949714910613&alt=media',
     'metageneration': '1',
     'name': 'nvsv7gjbtzya3cgsgxnocg/WhejVM7NTJe2fZ99Pdo2YA_billing_data_2020_NOVEMBER_1.avro',
     'selfLink': 'https://www.googleapis.com/storage/v1/b/clusterdata-dev/o/nvsv7gjbtzya3cgsgxnocg%2FWhejVM7NTJe2fZ99Pdo2YA_billing_data_2020_NOVEMBER_1.avro',
     'size': '1121493',
     'storageClass': 'STANDARD',
     'timeCreated': '2020-11-09T19:21:54.910Z',
     'timeStorageClassUpdated': '2020-11-09T19:21:54.910Z',
     'updated': '2020-11-09T19:21:54.910Z'
}
     
"""

PROJECTID = os.environ.get('GCP_PROJECT', 'ccm-play')
client = bigquery.Client(PROJECTID)
storage_client = storage.Client(PROJECTID)


def main(jsonData, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    print(jsonData)
    if os.environ.get('disabled', 'false').lower() == 'true':
        print("Function is disabled...")
        return
    filePath = jsonData['name']
    print(f"Processing file: {filePath}.")
    if not filePath.endswith(".avro"):
        print("Nothing to ingest")
        return

    # clusterdata-prod/WhejVM7NTJe2fZ99Pdo2YA/*.avro
    jsonData["fileName"] = filePath.split("/")[-1]
    jsonData["accountId"] = filePath.split("/")[-2]
    if jsonData["accountId"] in os.environ.get("disable_for_accounts", "").split(","):
        print_("Execution disabled for this account :%s" % jsonData["accountId"])
        return
    util.ACCOUNTID_LOG = jsonData["accountId"]

    # cluster table name
    jsonData["isHourly"] = False
    if jsonData["fileName"].startswith("billing_data_hourly"):
        jsonData["tableName"] = "clusterDataHourly"
        jsonData["tableNameAggregated"] = "clusterDataHourlyAggregated"
        jsonData["isHourly"] = True
    elif jsonData["fileName"].startswith("billing_data"):
        jsonData["tableName"] = "clusterData"
        jsonData["tableNameAggregated"] = "clusterDataAggregated"

    # Verify file format
    try:
        if jsonData["isHourly"]:
            jsonData["fileDate"] = re.search("billing_data_hourly_(.+?).avro", jsonData["fileName"]).group(1).split(
                "_")  # ['2020', 'NOVEMBER', '1', '1']
            jsonData["fileYear"] = int(jsonData["fileDate"][0])
            jsonData["fileMonth"] = MONTHMAP[jsonData["fileDate"][1]]
            jsonData["fileDay"] = int(jsonData["fileDate"][2])
            jsonData["fileHour"] = jsonData["fileDate"][3]
        else:
            jsonData["fileDate"] = re.search("billing_data_(.+?).avro", jsonData["fileName"]).group(1).split(
                "_")  # ['2020', 'NOVEMBER', '1']
            jsonData["fileYear"] = int(jsonData["fileDate"][0])
            jsonData["fileMonth"] = MONTHMAP[jsonData["fileDate"][1]]
            jsonData["fileDay"] = int(jsonData["fileDate"][2])
            jsonData["fileHour"] = "00"
    except Exception as e:
        # invalid file format
        print_(e)
        print_("Filename is invalid {}".format(jsonData["fileName"]))
        jsonData["fileDate"] = None

    jsonData["accountIdBQ"] = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())
    jsonData["datasetName"] = "BillingReport_%s" % jsonData["accountIdBQ"]
    jsonData["tableId"] = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])
    jsonData["tableIdAggregated"] = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableNameAggregated"])

    create_dataset_and_tables(jsonData)
    ingest_data_from_avro(jsonData)
    ingest_data_in_unified(jsonData)
    ingest_aggregated_data(jsonData)
    print_("Completed")


def create_dataset_and_tables(jsonData):
    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    cluster_data_table_ref = dataset.table(jsonData["tableName"])
    cluster_data_aggregated_table_ref = dataset.table(jsonData["tableNameAggregated"])
    unified_table_ref = dataset.table("unifiedTable")

    for table_ref in [cluster_data_table_ref, unified_table_ref, cluster_data_aggregated_table_ref]:
        if not if_tbl_exists(client, table_ref):
            print_("%s table does not exists, creating table..." % table_ref)
            createTable(client, table_ref)
        else:
            print_("%s table exists" % table_ref)
            if table_ref == cluster_data_aggregated_table_ref:
                alterTableAggregated(jsonData)


def ingest_data_from_avro(jsonData):
    """
    Loads data from Avro to clusterData/clusterDataHourly table
    """
    delete_existing_data(jsonData)
    job_config = bigquery.LoadJobConfig(
        source_format=bigquery.SourceFormat.AVRO,
        use_avro_logical_types=True,
        max_bad_records=0,
        write_disposition="WRITE_APPEND"  # default value
    )
    uri = "gs://" + jsonData["bucket"] + "/" + jsonData["accountId"] + "/" + jsonData["fileName"]  # or "*.avro"
    print_(uri)
    print_("Loading into %s table..." % jsonData["tableId"])
    load_job = client.load_table_from_uri(
        uri,
        jsonData["tableId"],
        job_config=job_config
    )  # Make an API request.
    try:
        load_job.result()  # Wait for the job to complete.
        table = client.get_table(jsonData["tableId"])
        print_("Total: {} rows in table {}".format(table.num_rows, jsonData["tableId"]))
        delete_from_gcs(jsonData)
    except Exception as e:
        print_(e, "WARN")
        # Probably the file was deleted in earlier runs


def delete_from_gcs(jsonData):
    """
    Deletes the blob from GCS
    :param jsonData:
    :return:
    """
    bucket_name = jsonData["bucket"]
    directory_name = jsonData["accountId"]
    bucket = storage_client.get_bucket(bucket_name)
    # list all objects in the directory
    blobs = bucket.list_blobs(prefix=directory_name)
    for blob in blobs:
        # kmpySmUISimoRrJL6NL73w/billing_data_2020_DECEMBER_10.avro
        if blob.name.endswith(jsonData["fileName"]):  # or .endswith(".avro"):
            print_("Deleting blob... : %s" % blob.name)
            blob.delete()
            print_("Deleted blob : %s" % blob.name)
            break


def delete_existing_data(jsonData):
    """
    Deletes data from clusterData/clusterDataHourly tables before insertion
    :param jsonData:
    :return:
    """
    if jsonData["fileDate"] == None:
        # Couldnt determine date from filename. return.
        return
    if jsonData["isHourly"]:
        print_("Deleting 1 hour data from %s..." % jsonData["tableId"])
        query = """DELETE FROM `%s` WHERE starttime = UNIX_MILLIS(TIMESTAMP(DATETIME(%s, %s, %s, %s, 00, 00)));
                """ % (jsonData["tableId"], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"],
                       jsonData["fileHour"])
    else:
        print_("Deleting 1 day data from %s..." % jsonData["tableId"])
        query = """DELETE FROM `%s` WHERE DATE(TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY))
                  = DATE(%s, %s, %s);
                """ % (jsonData["tableId"], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"])

    job_config = bigquery.QueryJobConfig(
        priority=bigquery.QueryPriority.BATCH,
        query_parameters=[
            bigquery.ScalarQueryParameter(
                "run_date",
                "DATE",
                datetime.datetime.utcnow().date(),
            )
        ]
    )
    query_job = client.query(query, job_config=job_config)
    try:
        # Experimental
        print_(query_job.job_id)
        count = 0
        while True:
            query_job = client.get_job(
                query_job.job_id, location=query_job.location
            )
            print_("Job {} is currently in state {}".format(query_job.job_id, query_job.state))
            if query_job.state in ["DONE", "SUCCESS"] or count >= 24: # 4 minutes
                break
            else:
                time.sleep(10)
                count += 1
        if query_job.state not in ["DONE", "SUCCESS"]:
            raise Exception("Timeout waiting for job in pending state")
    except Exception as e:
        print_(query)
        raise e
    print_("Deleted data from %s..." % jsonData["tableId"])


def ingest_data_in_unified(jsonData):
    if jsonData["tableName"] == "clusterDataHourly":
        return
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    print_("Loading into %s.unifiedTable..." % ds)
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) = DATE(%s, %s, %s)
                AND cloudProvider = "CLUSTER";
            INSERT INTO `%s.unifiedTable` (CLOUDPROVIDER, PRODUCT, STARTTIME, ENDTIME, COST, CPUBILLINGAMOUNT,
                MEMORYBILLINGAMOUNT, ACTUALIDLECOST, SYSTEMCOST, UNALLOCATEDCOST, NETWORKCOST, CLUSTERCLOUDPROVIDER,
                ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE, WORKLOADNAME, WORKLOADTYPE,
                INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, CLOUDSERVICENAME, LABELS)
            SELECT "CLUSTER" AS CLOUDPROVIDER,
                (CASE
                    WHEN CLUSTERTYPE = "K8S" THEN "Kubernetes Cluster"
                    ELSE "ECS Cluster"
                END) AS PRODUCT,
                TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY) as STARTTIME, TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(endtime), DAY) as ENDTIME,
                SUM(billingamount) AS COST, SUM(cpubillingamount) AS CPUBILLINGAMOUNT, SUM(memorybillingamount) AS MEMORYBILLINGAMOUNT,
                SUM(actualidlecost) AS ACTUALIDLECOST,  SUM(systemcost) AS SYSTEMCOST,  SUM(unallocatedcost) AS UNALLOCATEDCOST,
                SUM(networkcost) AS NETWORKCOST, cloudprovider AS CLUSTERCLOUDPROVIDER, accountid AS ACCOUNTID, clusterid AS CLUSTERID,
                clustername AS CLUSTERNAME, clustertype AS CLUSTERTYPE, region AS REGION, namespace AS NAMESPACE, workloadname AS WORKLOADNAME,
                workloadtype AS WORKLOADTYPE, instancetype AS INSTANCETYPE, appname AS APPID, servicename AS SERVICEID, envname AS ENVID,
                cloudproviderid AS CLOUDPROVIDERID, launchtype AS LAUNCHTYPE, cloudservicename AS CLOUDSERVICENAME, 
                ANY_VALUE(labels) as LABELS 
            FROM `%s` 
            WHERE DATE(TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY)) = DATE(%s, %s, %s) AND instancetype != "CLUSTER_UNALLOCATED"
            GROUP BY ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE,
                WORKLOADNAME,WORKLOADTYPE, INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, 
                CLOUDSERVICENAME, STARTTIME, ENDTIME, CLUSTERCLOUDPROVIDER;
    """ % (ds,  jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"], ds,
           jsonData['tableId'], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"])
    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter(
                "run_date",
                "DATE",
                datetime.datetime.utcnow().date(),
            )
        ]
    )
    query_job = client.query(query, job_config=job_config)
    try:
        print_(query_job.job_id)
        query_job.result()
    except Exception as e:
        print_(query)
        raise e
    print_("Loaded into %s.unifiedTable..." % ds)


def ingest_aggregated_data(jsonData):
    # cleanup older data
    print_("Ingesting aggregated data")
    print_("Cleaning up older aggregated data")
    DELETE_EXISTING_PREAGG = """DELETE FROM %s 
                                WHERE starttime = UNIX_MILLIS(TIMESTAMP(DATETIME(%s, %s, %s, %s, 00, 00))) ;
                            """ % (jsonData["tableIdAggregated"], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"],
                                   jsonData["fileHour"])
    try:
        query_job = client.query(DELETE_EXISTING_PREAGG)
        print_(query_job.job_id)
        query_job.result()
    except Exception as e:
        print_(DELETE_EXISTING_PREAGG)
        print_(e)

    # Gen preagg billing data
    PREAGG_QUERY = """INSERT INTO %s (MEMORYACTUALIDLECOST, CPUACTUALIDLECOST, STARTTIME, ENDTIME, BILLINGAMOUNT,
                        ACTUALIDLECOST, UNALLOCATEDCOST, SYSTEMCOST, STORAGEACTUALIDLECOST, STORAGEUNALLOCATEDCOST, 
                        STORAGEUTILIZATIONVALUE, STORAGEREQUEST, STORAGECOST, MEMORYUNALLOCATEDCOST, CPUUNALLOCATEDCOST, 
                        CPUBILLINGAMOUNT, MEMORYBILLINGAMOUNT, ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, 
                        REGION, NAMESPACE, WORKLOADNAME, WORKLOADTYPE,  INSTANCETYPE, APPID, SERVICEID, ENVID, 
                        CLOUDPROVIDERID, LAUNCHTYPE, CLOUDSERVICENAME, INSTANCENAME, CLOUDPROVIDER, NETWORKCOST, APPNAME,
                        SERVICENAME, ENVNAME, LABELS)  
                     SELECT SUM(MEMORYACTUALIDLECOST) as MEMORYACTUALIDLECOST, SUM(CPUACTUALIDLECOST) as CPUACTUALIDLECOST, 
                        max(STARTTIME) as STARTTIME, max(ENDTIME) as ENDTIME, sum(BILLINGAMOUNT) as BILLINGAMOUNT, 
                        sum(ACTUALIDLECOST) as ACTUALIDLECOST, sum(UNALLOCATEDCOST) as UNALLOCATEDCOST, 
                        sum(SYSTEMCOST) as SYSTEMCOST, SUM(STORAGEACTUALIDLECOST) as STORAGEACTUALIDLECOST, 
                        SUM(STORAGEUNALLOCATEDCOST) as STORAGEUNALLOCATEDCOST, MAX(STORAGEUTILIZATIONVALUE) as STORAGEUTILIZATIONVALUE, 
                        MAX(STORAGEREQUEST) as STORAGEREQUEST, SUM(STORAGECOST) as STORAGECOST, 
                        SUM(MEMORYUNALLOCATEDCOST) as MEMORYUNALLOCATEDCOST, SUM(CPUUNALLOCATEDCOST) as CPUUNALLOCATEDCOST, 
                        SUM(CPUBILLINGAMOUNT) as CPUBILLINGAMOUNT, SUM(MEMORYBILLINGAMOUNT) as MEMORYBILLINGAMOUNT, 
                        ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE, WORKLOADNAME, WORKLOADTYPE,  
                        INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, CLOUDSERVICENAME, INSTANCENAME,
                        CLOUDPROVIDER, SUM(networkcost) AS NETWORKCOST, APPNAME, SERVICENAME, ENVNAME, ANY_VALUE(labels) as LABELS
                    FROM %s 
                    WHERE STARTTIME = UNIX_MILLIS(TIMESTAMP(DATETIME(%s, %s, %s, %s, 00, 00))) 
                        AND INSTANCETYPE IN ("K8S_POD", 
                        "ECS_CONTAINER_INSTANCE", "ECS_TASK_EC2", "ECS_TASK_FARGATE", "K8S_POD_FARGATE") 
                    GROUP BY ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE, WORKLOADNAME, 
                        WORKLOADTYPE, INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, CLOUDSERVICENAME, 
                        INSTANCENAME, CLOUDPROVIDER, APPNAME, SERVICENAME, ENVNAME ;
                    """ % (jsonData["tableIdAggregated"], jsonData["tableId"], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"],
                           jsonData["fileHour"])

    # Call query
    print_("Ingesting aggregated data 1st query")
    try:
        query_job = client.query(PREAGG_QUERY)
        print_(query_job.job_id)
        query_job.result()
    except Exception as e:
        print_(PREAGG_QUERY)
        print_(e)

    PREAGG_QUERY_ID = """INSERT INTO %s (MEMORYACTUALIDLECOST, CPUACTUALIDLECOST, STARTTIME, ENDTIME, 
                            BILLINGAMOUNT, ACTUALIDLECOST, UNALLOCATEDCOST, SYSTEMCOST, STORAGEACTUALIDLECOST, STORAGEUNALLOCATEDCOST, 
                            STORAGEUTILIZATIONVALUE, STORAGEREQUEST, STORAGECOST, MEMORYUNALLOCATEDCOST, CPUUNALLOCATEDCOST, 
                            CPUBILLINGAMOUNT, MEMORYBILLINGAMOUNT, ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, 
                            NAMESPACE, WORKLOADNAME, WORKLOADTYPE,  INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, 
                            LAUNCHTYPE, CLOUDSERVICENAME, INSTANCEID, INSTANCENAME, CLOUDPROVIDER, NETWORKCOST, APPNAME,
                            SERVICENAME, ENVNAME, LABELS) 
                        SELECT SUM(MEMORYACTUALIDLECOST) as MEMORYACTUALIDLECOST, SUM(CPUACTUALIDLECOST) as CPUACTUALIDLECOST, 
                            max(STARTTIME) as STARTTIME, max(ENDTIME) as ENDTIME, sum(BILLINGAMOUNT) as BILLINGAMOUNT, 
                            sum(ACTUALIDLECOST) as ACTUALIDLECOST, sum(UNALLOCATEDCOST) as UNALLOCATEDCOST, sum(SYSTEMCOST) as SYSTEMCOST, 
                            SUM(STORAGEACTUALIDLECOST) as STORAGEACTUALIDLECOST, SUM(STORAGEUNALLOCATEDCOST) as STORAGEUNALLOCATEDCOST, 
                            MAX(STORAGEUTILIZATIONVALUE) as STORAGEUTILIZATIONVALUE, MAX(STORAGEREQUEST) as STORAGEREQUEST, 
                            SUM(STORAGECOST) as STORAGECOST, SUM(MEMORYUNALLOCATEDCOST) as MEMORYUNALLOCATEDCOST, 
                            SUM(CPUUNALLOCATEDCOST) as CPUUNALLOCATEDCOST, SUM(CPUBILLINGAMOUNT) as CPUBILLINGAMOUNT, 
                            SUM(MEMORYBILLINGAMOUNT) as MEMORYBILLINGAMOUNT, ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, 
                            REGION, NAMESPACE, WORKLOADNAME, WORKLOADTYPE,  INSTANCETYPE, APPID, SERVICEID, ENVID, 
                            CLOUDPROVIDERID, LAUNCHTYPE, CLOUDSERVICENAME, INSTANCEID, INSTANCENAME, CLOUDPROVIDER,
                            SUM(networkcost) AS NETWORKCOST, APPNAME, SERVICENAME, ENVNAME, ANY_VALUE(labels) as LABELS
                        FROM %s 
                        WHERE STARTTIME = UNIX_MILLIS(TIMESTAMP(DATETIME(%s, %s, %s, %s, 00, 00))) and INSTANCETYPE IN ("K8S_NODE", "K8S_PV") 
                        GROUP BY ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE, WORKLOADNAME, 
                            WORKLOADTYPE, INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, 
                            CLOUDSERVICENAME, INSTANCEID, INSTANCENAME, CLOUDPROVIDER, APPNAME, SERVICENAME, ENVNAME ;
                        """ % (jsonData["tableIdAggregated"], jsonData["tableId"], jsonData["fileYear"], jsonData["fileMonth"], jsonData["fileDay"],
                               jsonData["fileHour"])

    print_("Ingesting aggregated data 2nd query")
    try:
        query_job = client.query(PREAGG_QUERY_ID)
        print_(query_job.job_id)
        query_job.result()
    except Exception as e:
        print_(PREAGG_QUERY_ID)
        print_(e)


def alterTableAggregated(jsonData):
    print_("Altering %s Table" % jsonData["tableIdAggregated"])
    query = "ALTER TABLE `%s` \
            ADD COLUMN IF NOT EXISTS networkcost FLOAT64, \
            ADD COLUMN IF NOT EXISTS appname STRING, \
            ADD COLUMN IF NOT EXISTS servicename STRING, \
            ADD COLUMN IF NOT EXISTS envname STRING, \
            ADD COLUMN IF NOT EXISTS cloudprovider STRING, \
            ADD COLUMN IF NOT EXISTS labels ARRAY<STRUCT<key STRING, value STRING>>;" % (jsonData["tableIdAggregated"])
    try:
        query_job = client.query(query)
        print_(query_job.job_id)
        query_job.result()
    except Exception as e:
        print_(query)
        print_(e)
    else:
        print_("Finished altering %s table" % jsonData["tableIdAggregated"])


MONTHMAP = {
    "JANUARY": 1,
    "FEBRUARY": 2,
    "MARCH": 3,
    "APRIL": 4,
    "MAY": 5,
    "JUNE": 6,
    "JULY": 7,
    "AUGUST": 8,
    "SEPTEMBER": 9,
    "OCTOBER": 10,
    "NOVEMBER": 11,
    "DECEMBER": 12
}
