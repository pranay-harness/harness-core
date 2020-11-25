import datetime
import os
import re
from google.cloud import bigquery
from util import create_dataset, if_tbl_exists, createTable

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

def main(jsonData, context):
    """Entry point. Triggered by a change to a Cloud Storage bucket.
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

    # default to ce-prod. https://cloud.google.com/functions/docs/env-var
    projectName = os.environ.get('GCP_PROJECT', 'ce-prod')

    # clusterdata-prod/WhejVM7NTJe2fZ99Pdo2YA/*.avro
    fileName = filePath.split("/")[-1]
    accountId = filePath.split("/")[-2]
    if accountId in os.environ.get("disable_for_accounts", "").split(","):
        print("Execution disabled for this account :%s" % accountId)
        return
    accountId_ds = re.sub('[^0-9a-z]', '_', accountId.lower())
    datasetName = "BillingReport_%s" % (accountId_ds)
    # cluster table name
    tableName = clusterDataTableName = "%s.%s.%s" % (projectName, datasetName, "clusterData")

    jsonData["fileName"] = fileName
    try:
        jsonData["fileDate"] = re.search("billing_data_(.+?).avro", fileName).group(1).split("_") # ['2020', 'NOVEMBER', '1']
    except AttributeError:
        # invalid file format
        print("Filename is invalid {}".format(fileName))
        jsonData["fileDate"] = None
    jsonData["accountId"] = accountId
    jsonData["datasetName"] = datasetName
    jsonData["tableName"] = tableName
    jsonData["projectName"] = projectName

    client = bigquery.Client(projectName)
    dataset = client.dataset(datasetName)
    clusterDataTableRef = dataset.table("clusterData")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.%s.%s" % (jsonData["projectName"], jsonData["datasetName"], "unifiedTable")
    create_dataset(client, jsonData)
    if not if_tbl_exists(client, clusterDataTableRef):
        print("%s table does not exists, creating table..." % clusterDataTableRef)
        createTable(client, clusterDataTableName)
    else:
        print("%s table exists" % clusterDataTableRef)

    if not if_tbl_exists(client, unifiedTableRef):
        print("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableTableName)
    else:
        print("%s table exists" % unifiedTableRef)

    ingestDataFromAvroToClusterData(client, jsonData)
    ingestDataFromClusterDataToUnified(client, jsonData)
    client.close()


def ingestDataFromAvroToClusterData(client, jsonData):
    """Loads data from Avro to clusterData table
    """
    # table_id = "your-project.your_dataset.your_table_name"
    # TODO: Delete that days of data from clusterData table in case we reexecute something
    from google.cloud import bigquery
    deleteDataFromClusterData(client, jsonData)
    job_config = bigquery.LoadJobConfig(
        source_format=bigquery.SourceFormat.AVRO,
        use_avro_logical_types=True,
        max_bad_records=0,
        write_disposition="WRITE_APPEND" # default value
    )
    uri = "gs://" + jsonData["bucket"] + "/" + jsonData["accountId"] + "/" + jsonData["fileName"] # or "*.avro"
    print(uri)
    print("Loading into %s table..." % jsonData["tableName"])
    load_job = client.load_table_from_uri(
        uri,
        jsonData["tableName"],
        job_config=job_config
    )  # Make an API request.

    load_job.result()  # Wait for the job to complete.

    table = client.get_table(jsonData["tableName"])
    print("Total: {} rows in table {}".format(table.num_rows, jsonData["tableName"]))
    deleteFromGcs(jsonData)

def deleteFromGcs(jsonData):
    from google.cloud import storage
    bucket_name = jsonData["bucket"]
    directory_name = jsonData["accountId"]
    client = storage.Client()
    bucket = client.get_bucket(bucket_name)
    # list all objects in the directory
    blobs = bucket.list_blobs(prefix=directory_name)
    for blob in blobs:
        # kmpySmUISimoRrJL6NL73w/billing_data_2020_DECEMBER_10.avro
        if blob.name.endswith(jsonData["fileName"]): # or .endswith(".avro"):
             print("Deleting blob... : %s" % blob.name)
             blob.delete()
             print("Deleted blob : %s" % blob.name)
             break
          
def deleteDataFromClusterData(client, jsonData):
    from google.cloud import bigquery
    if jsonData["fileDate"] == None:
        # Couldnt determine date from filename. return.
        return
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    print("Deleting 1 day data from %s.clusterData..." % ds)
    # This needs to be the date as available in the filename
    year = int(jsonData["fileDate"][0])
    month = MONTHMAP[jsonData["fileDate"][1]]
    day = int(jsonData["fileDate"][2])
    query = """DELETE FROM `%s.clusterData` WHERE DATE(TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY))
                  = DATE(%s, %s, %s);
    """ % (ds, year, month, day)
    print(query)
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
    results = query_job.result()
    print("Deleted 1 day data from %s.clusterData..." % ds)


def ingestDataFromClusterDataToUnified(client, jsonData):
    from google.cloud import bigquery

    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    print("Loading into %s.unifiedTable..." % ds)
    # This needs to be the date as available in the filename
    year = int(jsonData["fileDate"][0])
    month = MONTHMAP[jsonData["fileDate"][1]]
    day = int(jsonData["fileDate"][2])
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
                workloadtype AS WORKLOADTYPE, instancetype AS INSTANCETYPE, appid AS APPID, serviceid AS SERVICEID, envid AS ENVID, 
                cloudproviderid AS CLOUDPROVIDERID, launchtype AS LAUNCHTYPE, cloudservicename AS CLOUDSERVICENAME, 
                ANY_VALUE(labels) as LABELS 
            FROM `%s.clusterData` 
            WHERE DATE(TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY)) = DATE(%s, %s, %s) AND instancetype != "CLUSTER_UNALLOCATED"
            GROUP BY ACCOUNTID, CLUSTERID, CLUSTERNAME, CLUSTERTYPE, REGION, NAMESPACE,
                WORKLOADNAME,WORKLOADTYPE, INSTANCETYPE, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, LAUNCHTYPE, 
                CLOUDSERVICENAME, STARTTIME, ENDTIME, CLUSTERCLOUDPROVIDER;
    """ % (ds, year, month, day, ds, ds, year, month, day)
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
    #print(query)
    results = query_job.result()
    print("Loaded into %s.unifiedTable..." % ds)

MONTHMAP = {
    "JANUARY" : 1,
    "FEBRUARY": 2,
    "MARCH"   : 3,
    "APRIL"   : 4,
    "MAY"     : 5,
    "JUNE"    : 6,
    "JULY"    : 7,
    "AUGUST"  : 8,
    "SEPTEMBER" : 9,
    "OCTOBER"   : 10,
    "NOVEMBER"  : 11,
    "DECEMBER"   : 12
}