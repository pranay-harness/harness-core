import json
import base64
import os
from google.cloud import bigquery
from google.cloud import storage
import datetime
import util
from util import create_dataset, if_tbl_exists, createTable, print_
from calendar import monthrange

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
"""

TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"

def main(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the event.
    """
    print(event)
    data = base64.b64decode(event['data']).decode('utf-8')
    jsonData = json.loads(data)
    print(jsonData)
    # This is available only in runtime python 3.7, go 1.11
    jsonData["projectName"] = os.environ.get('GCP_PROJECT', 'ce-prod-274307')

    client = bigquery.Client(jsonData["projectName"])
    #create_dataset(client, jsonData)
    util.ACCOUNTID_LOG = jsonData.get("accountIdOrig")
    create_dataset(client, jsonData["datasetName"], jsonData.get("accountIdOrig"))
    dataset = client.dataset(jsonData["datasetName"])
    awsCurTableref = dataset.table(jsonData["tableName"])
    awsCurTableName =  TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], jsonData["tableName"])
    preAggragatedTableRef = dataset.table("preAggregated")
    preAggragatedTableTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "unifiedTable")
    if not if_tbl_exists(client, awsCurTableref):
        createTable(client, awsCurTableName)
    if not if_tbl_exists(client, unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableTableName)
    else:
        print_("%s table exists" % unifiedTableTableName)

    if not if_tbl_exists(client, preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggragatedTableTableName)
    else:
        print_("%s table exists" % preAggragatedTableTableName)

    # start streaming the data from the gcs
    print_("%s table exists. Starting to write data from gcs into it..." % jsonData["tableName"])
    try:
        ingestDataFromCsvToAwsCurTableTable(client, jsonData)
    except Exception as e:
        print_(e, "WARN")
        return
    ingestDataToAwsCurTable(client, jsonData)
    ingestDataToPreaggregatedTable(client, jsonData)
    ingestDataInUnifiedTableTable(client, jsonData)


def ingestDataFromCsvToAwsCurTableTable(client, jsonData):
    job_config = bigquery.LoadJobConfig(
        skip_leading_rows=1,
        field_delimiter=",",
        ignore_unknown_values=True,
        source_format="CSV",
        allow_quoted_newlines=True,
        allow_jagged_rows=True
    )
    uris = ["gs://" + jsonData["bucket"] + "/" + "/".join(jsonData["fileName"].split("/")[:-1]) + "/*.csv",
            "gs://" + jsonData["bucket"] + "/" + "/".join(jsonData["fileName"].split("/")[:-1]) + "/*.csv.gz"]
    print_("Ingesting all CSVs from %s" % uris)
    # format: ce-prod-274307:BillingReport_wfhxhd0rrqwoo8tizt5yvw.awsCurTable_2020_04
    table_id = "%s.%s" % (jsonData["datasetName"], jsonData["tableName"])
    print_(table_id)
    print_("Loading into %s table..." % table_id)
    load_job = client.load_table_from_uri(
        uris,
        table_id,
        job_config=job_config
    )  # Make an API request.

    load_job.result()  # Wait for the job to complete.

    table = client.get_table(table_id)
    print_("Total {} rows in table {}".format(table.num_rows, table_id))
    storage_client = storage.Client(jsonData["projectId"])
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix="/".join(jsonData["fileName"].split("/")[:-1])
    )
    for blob in blobs:
        if blob.name.endswith(".csv") or blob.name.endswith(".csv.gz"):
            blob.delete()
            print_("Blob {} deleted.".format(blob.name))

def ingestDataToAwsCurTable(client, jsonData):
    ds = "%s.%s" % (jsonData["projectId"], jsonData["datasetName"])
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
               AS tags FROM `%s.awsCurTable_%s` table;
     """ % (ds, jsonData["tableSuffix"])

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
        time_partitioning = time_partitioning
    )
    job_config.destination = tableName
    job_config.write_disposition = "WRITE_TRUNCATE"
    query_job = client.query(query, job_config=job_config)
    query_job.result()
    print_("Loaded into %s table..." % tableName)


def ingestDataToPreaggregatedTable(client, jsonData):
    ds = "%s.%s" % (jsonData["projectId"], jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "preAggregated")
    year, month = jsonData["tableSuffix"].split('_')
    date_start =  "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s preAggregated table..." % tableName)
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s' AND cloudProvider = "AWS";
               INSERT INTO `%s.preAggregated` (startTime, awsBlendedRate,awsBlendedCost,awsUnblendedRate, awsUnblendedCost, cost,
                                               awsServicecode, region,awsAvailabilityzone,awsUsageaccountid,awsInstancetype,awsUsagetype,cloudProvider)
               SELECT TIMESTAMP_TRUNC(usagestartdate, DAY) as startTime, min(blendedrate) AS awsBlendedRate, sum(blendedcost) AS awsBlendedCost,
                    min(unblendedrate) AS awsUnblendedRate, sum(unblendedcost) AS awsUnblendedCost, sum(unblendedcost) AS cost,
                    productname AS awsServicecode, region, availabilityzone AS awsAvailabilityzone, usageaccountid AS awsUsageaccountid,
                    instancetype AS awsInstancetype, usagetype AS awsUsagetype, "AWS" AS cloudProvider 
               FROM `%s.awscur_%s` WHERE lineitemtype != 'Tax'
               GROUP BY awsServicecode, region, awsAvailabilityzone, awsUsageaccountid, awsInstancetype, awsUsagetype, startTime;
     """ % (ds, date_start, date_end, ds, ds, jsonData["tableSuffix"])
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


def ingestDataInUnifiedTableTable(client, jsonData):
    ds = "%s.%s" % (jsonData["projectId"], jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "unifiedTable")
    year, month = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s table..." % tableName)
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s'  AND cloudProvider = "AWS";
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
     """ % (ds, date_start, date_end, ds, ds, jsonData["tableSuffix"])

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