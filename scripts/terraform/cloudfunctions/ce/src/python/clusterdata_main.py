import datetime
import os
import re
from google.cloud import bigquery
import util
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
        print_("Execution disabled for this account :%s" % accountId)
        return
    util.ACCOUNTID_LOG = accountId
    accountId_ds = re.sub('[^0-9a-z]', '_', accountId.lower())
    datasetName = "BillingReport_%s" % (accountId_ds)
    # cluster table name
    tableName = clusterDataTableName = "%s.%s.%s" % (projectName, datasetName, "clusterData")

    jsonData["fileName"] = fileName
    try:
        jsonData["fileDate"] = re.search("billing_data_(.+?).avro", fileName).group(1).split("_") # ['2020', 'NOVEMBER', '1']
    except AttributeError:
        # invalid file format
        print_("Filename is invalid {}".format(fileName))
        jsonData["fileDate"] = None
    jsonData["accountId"] = accountId.strip()
    jsonData["datasetName"] = datasetName
    jsonData["tableName"] = tableName
    jsonData["projectName"] = projectName

    client = bigquery.Client(projectName)
    dataset = client.dataset(datasetName)
    clusterDataTableRef = dataset.table("clusterData")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.%s.%s" % (jsonData["projectName"], jsonData["datasetName"], "unifiedTable")
    #create_dataset(client, jsonData)
    create_dataset(client, jsonData["datasetName"], jsonData.get("accountIdOrig"))
    if not if_tbl_exists(client, clusterDataTableRef):
        print_("%s table does not exists, creating table..." % clusterDataTableRef)
        createTable(client, clusterDataTableRef)
    else:
        alterClusterTable(client, jsonData)
        print_("%s table exists" % clusterDataTableRef)

    if not if_tbl_exists(client, unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableRef)
    else:
        print_("%s table exists" % unifiedTableRef)

    ingestDataFromAvroToClusterData(client, jsonData)
    ingestDataFromClusterDataToUnified(client, jsonData)
    # These are now done in corresponding AWS and GCP CFs.
    #loadIntoUnifiedGCP(client, jsonData)
    #loadIntoUnifiedAWS(client, jsonData)
    client.close()

def alterClusterTable(client, jsonData):
    print_("Altering CLuster Data Table")
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    query = "ALTER TABLE `%s.clusterData` ADD COLUMN IF NOT EXISTS storageactualidlecost FLOAT64, ADD COLUMN IF NOT EXISTS storageunallocatedcost FLOAT64, ADD COLUMN IF NOT EXISTS storageutilizationvalue FLOAT64, ADD COLUMN IF NOT EXISTS storagerequest FLOAT64, ADD COLUMN IF NOT EXISTS storagembseconds FLOAT64, ADD COLUMN IF NOT EXISTS storagecost FLOAT64;" % (ds)


    try:
        query_job = client.query(query)
        results = query_job.result()
    except Exception as e:
        # Error Running Alter Query
        print_(e, "WARN")
    else:
        print_("Finished Altering CLuster Data Table")


def ingestDataFromAvroToClusterData(client, jsonData):
    """Loads data from Avro to clusterData table
    """
    # table_id = "your-project.your_dataset.your_table_name"
    from google.cloud import bigquery
    deleteDataFromClusterData(client, jsonData)
    job_config = bigquery.LoadJobConfig(
        source_format=bigquery.SourceFormat.AVRO,
        use_avro_logical_types=True,
        max_bad_records=0,
        write_disposition="WRITE_APPEND" # default value
    )
    uri = "gs://" + jsonData["bucket"] + "/" + jsonData["accountId"] + "/" + jsonData["fileName"] # or "*.avro"
    print_(uri)
    print_("Loading into %s table..." % jsonData["tableName"])
    load_job = client.load_table_from_uri(
        uri,
        jsonData["tableName"],
        job_config=job_config
    )  # Make an API request.
    try:
        load_job.result()  # Wait for the job to complete.
    except Exception as e:
        print_(e, "WARN")
        # Probably the file was deleted in earlier runs

    table = client.get_table(jsonData["tableName"])
    print_("Total: {} rows in table {}".format(table.num_rows, jsonData["tableName"]))
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
            print_("Deleting blob... : %s" % blob.name)
            blob.delete()
            print_("Deleted blob : %s" % blob.name)
            break

def loadIntoUnifiedGCP(client, jsonData):
    print_("Loading into unifiedTable table for GCP...")
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) <= DATE_SUB(@run_date , INTERVAL 1 DAY) AND
                    DATE(startTime) >= DATE_SUB(@run_date , INTERVAL 6 DAY) AND cloudProvider = "GCP";
           INSERT INTO `%s.unifiedTable` (product, cost, gcpProduct,gcpSkuId,gcpSkuDescription, startTime, gcpProjectId,
                region,zone,gcpBillingAccountId,cloudProvider, discount, labels)
                SELECT service.description AS product, cost AS cost, service.description AS gcpProduct, sku.id AS gcpSkuId,
                     sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, project.id AS gcpProjectId,
                     location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, "GCP" AS cloudProvider, credits.amount as discount, labels AS labels
                FROM `%s.gcp_billing_export*` LEFT JOIN UNNEST(credits) as credits
                WHERE DATE(usage_start_time) <= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) AND
                     DATE(usage_start_time) >= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 6 DAY) ;
    """ % (ds, ds, ds)

    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter(
                "run_date",
                "DATE",
                datetime.datetime.utcnow().date(),
            )
        ]
    )
    try:
        query_job = client.query(query, job_config=job_config)
        results = query_job.result()
    except Exception as e:
        # For this account table GCP billing data isnt present
        print_(e, "WARN")
    else:
        #print(query)
        print_("Loaded into unifiedTable table for GCP...")


def loadIntoUnifiedAWS(client, jsonData):
    """
    Assumption: unifiedTable should be created at this point.
    """
    from google.cloud import bigquery
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    print_(ds)
    dataset = client.get_dataset(ds)
    table_ref = dataset.table("unifiedTable")
    # In the new BigQuery dataset, create a reference to a new table for
    # storing the query results.
    print_("Loading into unifiedTable table for AWS...")
    # Configure the query job.
    job_config = bigquery.QueryJobConfig()

    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= DATE_SUB(@run_date , INTERVAL 6 DAY) AND cloudProvider = "AWS";
               INSERT INTO `%s.unifiedTable` (product, startTime,
                    awsBlendedRate,awsBlendedCost,awsUnblendedRate, awsUnblendedCost, cost, awsServicecode,
                    region,awsAvailabilityzone,awsUsageaccountid,awsInstancetype,awsUsagetype,cloudProvider, labels)
               SELECT productname AS product, TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, blendedrate AS
                    awsBlendedRate, blendedcost AS awsBlendedCost, unblendedrate AS awsUnblendedRate, unblendedcost AS
                    awsUnblendedCost, unblendedcost AS cost, productname AS awsServicecode, region, availabilityzone AS
                    awsAvailabilityzone, usageaccountid AS awsUsageaccountid, instancetype AS awsInstancetype, usagetype
                    AS awsUsagetype, "AWS" AS cloudProvider, tags AS labels FROM `%s.awscur_2020_*`
               WHERE lineitemtype != 'Tax' AND
                    TIMESTAMP_TRUNC(usagestartdate, DAY) >= TIMESTAMP_TRUNC(TIMESTAMP_SUB(TIMESTAMP (@run_date),
                    INTERVAL 6 DAY), DAY);
    """ % (ds, ds, ds)

    # Configure the query job.
    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter(
                "run_date",
                "DATE",
                datetime.datetime.utcnow().date(),
            )
        ]
    )
    try:
        query_job = client.query(query, job_config=job_config)
        results = query_job.result()
    except Exception as e:
        # For this account table AWS billing data isnt present
        print_(e, "WARN")
    else:
        print_("Loaded into unified table for AWS...")

def deleteDataFromClusterData(client, jsonData):
    from google.cloud import bigquery
    if jsonData["fileDate"] == None:
        # Couldnt determine date from filename. return.
        return
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    print_("Deleting 1 day data from %s.clusterData..." % ds)
    # This needs to be the date as available in the filename
    year = int(jsonData["fileDate"][0])
    month = MONTHMAP[jsonData["fileDate"][1]]
    day = int(jsonData["fileDate"][2])
    query = """DELETE FROM `%s.clusterData` WHERE DATE(TIMESTAMP_TRUNC(TIMESTAMP_MILLIS(starttime), DAY))
                  = DATE(%s, %s, %s);
    """ % (ds, year, month, day)
    print_(query)
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
    print_("Deleted 1 day data from %s.clusterData..." % ds)


def ingestDataFromClusterDataToUnified(client, jsonData):
    from google.cloud import bigquery

    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    print_("Loading into %s.unifiedTable..." % ds)
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
                workloadtype AS WORKLOADTYPE, instancetype AS INSTANCETYPE, appname AS APPID, servicename AS SERVICEID, envname AS ENVID,
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
    print_("Loaded into %s.unifiedTable..." % ds)

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