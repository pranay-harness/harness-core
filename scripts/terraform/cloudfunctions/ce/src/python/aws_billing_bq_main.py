import json
import base64
import os
import re
import datetime
import util

from util import create_dataset, if_tbl_exists, createTable, print_, TABLE_NAME_FORMAT
from calendar import monthrange
from google.cloud import bigquery
from google.cloud import storage

"""
{
	"accountId": "nvsv7gjbtzya3cgsgxnocg",
	"accountIdOrig": "nVS...",
	"bucket": "nikunjtestbucket",
	"datasetName": "BillingReport_nvsv7gjbtzya3cgsgxnocg",
	"fileName": "AROAY2UX4LR3HUT7WH7DG:NVsV7gjbTZyA3CgSgXNOcg/PGnxKAheSKWY30YHcgSNLg/Harness/20201101-20201201/Harness-Manifest.json",
	"projectId": "ccm-play",
	"tableName": "awsCurTable_2020_11",
	"tableSuffix" : "2020_11"
}

{   
    "accountId": "o0yschY0RrGZJ2JFGEpvdw",
    "path": "AROAXVZVVGMCF7KFQSJ37:o0yschY0RrGZJ2JFGEpvdw/mg7Qs7PuQxAgqg3aNzau0x/harness_cloud_cost_demo/20210501-20210601" or 
            "AROAY2UX4LR3HUT7WH7DG:NVsV7gjbTZyA3CgSgXNOcg/PGnxKAheSKWY30YHcgSNLg/Harness/20201101-20201201/<versioned>/",
    "bucket": "awscustomerbillingdata-dev"
}
"""

PROJECTID = os.environ.get('GCP_PROJECT', 'ccm-play')
client = bigquery.Client(PROJECTID)
storage_client = storage.Client(PROJECTID)


def main(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    print(event)
    data = base64.b64decode(event['data']).decode('utf-8')
    event_json = json.loads(data)
    jsonData = event_json.get("data", {}).get("message", {})
    print(jsonData)

    # Set accountid for GCP logging
    util.ACCOUNTID_LOG = jsonData.get("accountId")

    ps = jsonData["path"].split("/")
    if len(ps) == 4:
        monthfolder = ps[-1]  # last folder in path
        jsonData["cleanuppath"] = jsonData["path"]
    elif len(ps) == 5:
        monthfolder = ps[-2]  # second last folder in path
        jsonData["cleanuppath"] = "/".join(ps[:-1])

    reportYear = monthfolder.split("-")[0][:4]
    reportMonth = monthfolder.split("-")[0][4:6]

    connector_id = ps[1]  # second from beginning is connector id in mongo

    accountIdBQ = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())
    jsonData["datasetName"] = "BillingReport_%s" % (accountIdBQ)

    jsonData["tableSuffix"] = "%s_%s_%s" % (reportYear, reportMonth, connector_id)
    jsonData["tableName"] = f"awsBilling_{jsonData['tableSuffix']}"
    jsonData["tableId"] = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])

    # Create awsBilling_ table from Manifest JSON file.
    create_dataset_and_tables(jsonData)
    # start streaming the data from the gcs
    try:
        ingest_data_from_csv(jsonData)
    except Exception as e:
        print_(e, "WARN")
        return
    get_unique_accountids(jsonData)
    ingest_data_to_awscur(jsonData)
    ingest_data_to_preagg(jsonData)
    ingest_data_to_unified(jsonData)


def create_dataset_and_tables(jsonData):
    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])
    create_table_from_manifest(jsonData)

    preAggragatedTableRef = dataset.table("preAggregated")
    preAggregatedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "unifiedTable")

    if not if_tbl_exists(client, unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableTableName)
    else:
        print_("%s table exists" % unifiedTableTableName)

    if not if_tbl_exists(client, preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggregatedTableTableName)
    else:
        print_("%s table exists" % preAggregatedTableTableName)


def create_table_from_manifest(jsonData):
    # Read the CSV from GCS as string
    manifestdata = {"columns": []}
    try:
        blobs = storage_client.list_blobs(
            jsonData["bucket"], prefix=jsonData["path"])
        blob_to_delete = None
        for blob in blobs:
            # To avoid picking up sub directory json files
            if not jsonData["path"] == "/".join(blob.name.split("/")[:-1]):
                continue
            if blob.name.endswith("Manifest.json"):
                print_(blob.name)
                data = blob.download_as_string()
                data = data.decode('utf-8')
                manifestdata = json.loads(data)
                print_("Found manifest data: %s" % manifestdata)
                blob_to_delete = blob
                break
    except Exception as e:
        print_(e)
        raise e

    # Prepare table schema from manifest json
    reg = re.compile("[^a-zA-Z0-9_]")
    map_tags = {}
    schema = []
    for column in manifestdata["columns"]:
        name = column["name"].lower()
        if reg.search(name):
            # This must be a TAG ex. aws:autoscaling:groupName
            name = "TAG_" + re.sub(reg, "_", column["name"])
        name_for_map = column["name"].lower()
        try:
            name = name + "_" + str(map_tags[name_for_map])
            map_tags[name_for_map] += 1
        except:
            map_tags[name_for_map] = 1
        data_type = get_mapped_data_column(column["type"])
        schema.append(bigquery.SchemaField(name, data_type, "NULLABLE"))

    # Create table
    try:
        if len(schema) != 0:
            # Delete older table only when new manifest format is available
            client.delete_table(jsonData["tableId"], not_found_ok=True)
        table = client.create_table(bigquery.Table(jsonData["tableId"], schema=schema))
        print_("Created table from blob {} {}.{}.{}".format(blob_to_delete.name, table.project, table.dataset_id, table.table_id))
        blob_to_delete.delete()
    except Exception as e:
        print_("Error while creating table\n {}".format(e), "ERROR")


def get_mapped_data_column(data_type):
    if data_type == "String":
        modified_data_type = "STRING"
    elif data_type == "OptionalString":
        modified_data_type = "STRING"
    elif data_type == "Interval":
        modified_data_type = "STRING"
    elif data_type == "DateTime":
        modified_data_type = "TIMESTAMP"
    elif data_type == "BigDecimal":
        modified_data_type = "FLOAT"
    elif data_type == "OptionalBigDecimal":
        modified_data_type = "FLOAT"
    else:
        modified_data_type = "STRING"
    return modified_data_type


def ingest_data_from_csv(jsonData):
    job_config = bigquery.LoadJobConfig(
        skip_leading_rows=1,
        field_delimiter=",",
        ignore_unknown_values=True,
        source_format="CSV",
        allow_quoted_newlines=True,
        allow_jagged_rows=True
    )
    uris = ["gs://" + jsonData["bucket"] + "/" + jsonData["path"] + "/*.csv",
            "gs://" + jsonData["bucket"] + "/" + jsonData["path"] + "/*.csv.gz",
            "gs://" + jsonData["bucket"] + "/" + jsonData["path"] + "/*.csv.zip"]
    print_("Ingesting all CSVs from %s" % jsonData["path"])
    print_("Loading into %s table..." % jsonData["tableId"])
    load_job = client.load_table_from_uri(
        uris,
        jsonData["tableId"],
        job_config=job_config
    )  # Make an API request.

    load_job.result()  # Wait for the job to complete.

    table = client.get_table(jsonData["tableId"])
    print_("Total {} rows in table {}".format(table.num_rows, jsonData["tableId"]))
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix=jsonData["cleanuppath"]
    )
    print_("Cleaning up all csvs in this path: %s" % jsonData["cleanuppath"])
    for blob in blobs:
        if blob.name.endswith(".csv") or blob.name.endswith(".csv.gz"):
            blob.delete()
            print_("Blob {} deleted.".format(blob.name))


def ingest_data_to_awscur(jsonData):
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    # In the new BigQuery dataset, create a reference to a new table for
    # storing the query results.
    tableName = "%s.awscur_%s" % (ds, jsonData["tableSuffix"])
    print_("Loading into %s table..." % tableName)

    query = """SELECT resourceid, usagestartdate, productname, productfamily, servicecode, blendedrate, blendedcost, 
                    unblendedrate, unblendedcost, region, availabilityzone, usageaccountid, instancetype, usagetype, 
                    lineitemtype, effectivecost, 
                    ( SELECT ARRAY_AGG(STRUCT( regexp_replace(REGEXP_EXTRACT(unpivotedData, '[^"]*'), 'TAG_' , '') AS key , 
                         regexp_replace(REGEXP_EXTRACT(unpivotedData, r':\"[^"]*'), ':"', '') AS value )) 
                         FROM UNNEST(( SELECT REGEXP_EXTRACT_ALL(json, 'TAG_' || r'[^:]+:\"[^"]+\"') FROM (SELECT TO_JSON_STRING(table) json))) unpivotedData) 
               AS tags FROM `%s` table;
     """ % (jsonData["tableId"])

    # Configure the query job.
    time_partitioning = bigquery.table.TimePartitioning()
    time_partitioning.field = 'usagestartdate'
    time_partitioning.type_ = bigquery.table.TimePartitioningType.DAY
    job_config = bigquery.QueryJobConfig(
        query_parameters=[
            bigquery.ScalarQueryParameter(
                "run_date",
                "DATE",
                datetime.datetime.utcnow().date(),
            )
        ],
        time_partitioning=time_partitioning
    )
    job_config.destination = tableName
    job_config.write_disposition = "WRITE_TRUNCATE"
    query_job = client.query(query, job_config=job_config)
    query_job.result()
    print_("Loaded into %s table..." % tableName)


def get_unique_accountids(jsonData):
    # Get unique subsids from main awsBilling table
    query = """ 
            SELECT DISTINCT(usageaccountid) FROM `%s`;
            """ % (jsonData["tableId"])
    try:
        query_job = client.query(query)
        results = query_job.result()  # wait for job to complete
        usageaccountid = []
        for row in results:
            usageaccountid.append(row.usageaccountid)
        jsonData["usageaccountid"] = ", ".join(f"'{w}'" for w in usageaccountid)
    except Exception as e:
        print_("Failed to retrieve distinct subsids", "WARN")
        jsonData["usageaccountid"] = ""
        raise e
    print_("Found unique usageaccountid %s" % usageaccountid)


def ingest_data_to_preagg(jsonData):
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "preAggregated")
    year, month, _ = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s preAggregated table..." % tableName)
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s' AND cloudProvider = "AWS"
                AND awsUsageAccountId IN (%s);
               INSERT INTO `%s.preAggregated` (startTime, awsBlendedRate,awsBlendedCost,awsUnblendedRate, awsUnblendedCost, cost,
                                               awsServicecode, region,awsAvailabilityzone,awsUsageaccountid,awsInstancetype,awsUsagetype,cloudProvider)
               SELECT TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, min(blendedrate) AS awsBlendedRate, sum(blendedcost) AS awsBlendedCost,
                    min(unblendedrate) AS awsUnblendedRate, sum(unblendedcost) AS awsUnblendedCost, sum(unblendedcost) AS cost,
                    productname AS awsServicecode, region, availabilityzone AS awsAvailabilityzone, usageaccountid AS awsUsageaccountid,
                    instancetype AS awsInstancetype, usagetype AS awsUsagetype, "AWS" AS cloudProvider 
               FROM `%s.awscur_%s` WHERE lineitemtype != 'Tax'
               GROUP BY awsServicecode, region, awsAvailabilityzone, awsUsageaccountid, awsInstancetype, awsUsagetype, startTime;
     """ % (ds, date_start, date_end, jsonData["usageaccountid"], ds, ds, jsonData["tableSuffix"])
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
    query_job.result()
    print_("Loaded into %s table..." % tableName)


def ingest_data_to_unified(jsonData):
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "unifiedTable")
    year, month, _ = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s table..." % tableName)
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s'  AND cloudProvider = "AWS"
                AND awsUsageAccountId IN (%s);
               INSERT INTO `%s.unifiedTable` (product, startTime,
                    awsBlendedRate,awsBlendedCost,awsUnblendedRate, awsUnblendedCost, cost, awsServicecode,
                    region,awsAvailabilityzone,awsUsageaccountid,awsInstancetype,awsUsagetype,cloudProvider, labels)
               SELECT productname AS product, TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, blendedrate AS
                    awsBlendedRate, blendedcost AS awsBlendedCost, unblendedrate AS awsUnblendedRate, unblendedcost AS
                    awsUnblendedCost, unblendedcost AS cost, productname AS awsServicecode, region, availabilityzone AS
                    awsAvailabilityzone, usageaccountid AS awsUsageaccountid, instancetype AS awsInstancetype, usagetype
                    AS awsUsagetype, "AWS" AS cloudProvider, tags AS labels 
               FROM `%s.awscur_%s` 
               WHERE lineitemtype != 'Tax'; 
     """ % (ds, date_start, date_end, jsonData["usageaccountid"], ds, ds, jsonData["tableSuffix"])

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
    query_job = client.query(query, job_config=job_config)
    query_job.result()
    print_("Loaded into %s table..." % tableName)
