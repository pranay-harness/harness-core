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
	"bucket": "azurecustomerbillingdata-qa",
	"contentType": "text/csv",
	"crc32c": "txz7fA==",
	"etag": "CNTZ/5Kmwe4CEAE=",
	"generation": "1611928646446292",
	"id": "azurecustomerbillingdata-qa/kmpySmUISimoRrJL6NL73w/JUKVZIGKQzCVKXYbDhmM_g/20210101-20210131/cereportnikunj_0e8fa366-608d-4aa8-8fb7-dc8fef9fe717.csv/1611928646446292",
	"kind": "storage#object",
	"md5Hash": "/wftWBBbhax7CKIlPjL/DA==",
	"mediaLink": "https://www.googleapis.com/download/storage/v1/b/azurecustomerbillingdata-qa/o/kmpySmUISimoRrJL6NL73w%2FJUKVZIGKQzCVKXYbDhmM_g%2F20210101-20210131%2Fcereportnikunj_0e8fa366-608d-4aa8-8fb7-dc8fef9fe717.csv?generation=1611928646446292&alt=media",
	"metageneration": "1",
	"name": "kmpySmUISimoRrJL6NL73w/JUKVZIGKQzCVKXYbDhmM_g/20210101-20210131/cereportnikunj_0e8fa366-608d-4aa8-8fb7-dc8fef9fe717.csv",
	"selfLink": "https://www.googleapis.com/storage/v1/b/azurecustomerbillingdata-qa/o/kmpySmUISimoRrJL6NL73w%2FJUKVZIGKQzCVKXYbDhmM_g%2F20210101-20210131%2Fcereportnikunj_0e8fa366-608d-4aa8-8fb7-dc8fef9fe717.csv",
	"size": "3085577",
	"storageClass": "STANDARD",
	"timeCreated": "2021-01-29T13:57:26.523Z",
	"timeStorageClassUpdated": "2021-01-29T13:57:26.523Z",
	"updated": "2021-01-29T13:57:26.523Z",
	"accountId": "kmpysmuisimorrjl6nl73w",
	"projectName": "ce-qa-274307",
	"tableName": "ce-qa-274307.BillingReport_kmpysmuisimorrjl6nl73w.azureBilling_2021_01",
	"datasetName": "BillingReport_kmpysmuisimorrjl6nl73w",
	"tableSuffix": "2021_01",
	"accountIdOrig": "kmpySmUISimoRrJL6NL73w"
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

    jsonData["tableName"] = f"azureBilling_{jsonData['tableSuffix']}"
    client = bigquery.Client(jsonData["projectName"])
    util.ACCOUNTID_LOG = jsonData.get("accountIdOrig")
    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    preAggragatedTableRef = dataset.table("preAggregated")
    preAggragatedTableTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "preAggregated")

    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "unifiedTable")

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
        ingestDataFromCsvToAzureTable(client, jsonData)
    except Exception as e:
        print_(e, "WARN")
        return
    ingestDataToPreaggregatedTable(client, jsonData)
    ingestDataInUnifiedTableTable(client, jsonData)


def ingestDataFromCsvToAzureTable(client, jsonData):
    # Determine blob of highest size
    storage_client = storage.Client(jsonData["projectName"])
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix="/".join(jsonData["name"].split("/")[:-1])
    )
    maxsize = 0
    csvtoingest = None
    for blob in blobs:
        if blob.name.endswith(".csv") or blob.name.endswith(".csv.gz"):
            if blob.size > maxsize:
                maxsize = blob.size
                csvtoingest = blob.name
    if not csvtoingest:
        print_("No CSV to insert. GCS bucket might be empty", "WARN")
        return
    print_(csvtoingest)

    job_config = bigquery.LoadJobConfig(
        autodetect=True,
        skip_leading_rows=1,
        field_delimiter=",",
        ignore_unknown_values=True,
        source_format="CSV",
        allow_quoted_newlines=True,
        allow_jagged_rows=True,
        write_disposition='WRITE_TRUNCATE' #If the table already exists, BigQuery overwrites the table data
    )
    uris = ["gs://" + jsonData["bucket"] + "/" + csvtoingest]
    print_("Ingesting CSV from %s" % uris)
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
    # cleanup the processed blobs
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix="/".join(jsonData["name"].split("/")[:-1])
    )
    for blob in blobs:
        if blob.name.endswith(".csv") or blob.name.endswith(".csv.gz"):
            blob.delete()
            print("Blob {} deleted.".format(blob.name))


def ingestDataToPreaggregatedTable(client, jsonData):
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "preAggregated")
    year, month = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s preAggregated table..." % tableName)
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s' AND cloudProvider = "AZURE";
           INSERT INTO `%s.preAggregated` (startTime, azureResourceRate, cost,
                                           azureServiceName, region, azureSubscriptionGuid,
					                        cloudProvider)
           SELECT TIMESTAMP(UsageDateTime) as startTime, min(ResourceRate) AS azureResourceRate, sum(PreTaxCost) AS cost,
                ServiceName AS azureServiceName, ResourceLocation as region, SubscriptionGuid as azureSubscriptionGuid,
                "AZURE" AS cloudProvider
           FROM `%s.azureBilling_%s`
           GROUP BY azureServiceName, region, azureSubscriptionGuid, startTime;
    """ % (ds, date_start, date_end, ds, ds, jsonData["tableSuffix"])
    #print(query)
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
    createUDF(client, jsonData["projectName"])
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "unifiedTable")
    year, month = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s table..." % tableName)
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s'  AND cloudProvider = "AZURE";
           INSERT INTO `%s.unifiedTable`
                (product, startTime, cost,
                azureMeterCategory, azureMeterSubcategory, azureMeterId,
                azureMeterName, azureResourceType, azureServiceTier,
                azureInstanceId, region, azureResourceGroup,
                azureSubscriptionGuid, azureServiceName,
                cloudProvider, labels)
           SELECT ServiceName AS product, TIMESTAMP(UsageDateTime) as startTime, PreTaxCost AS cost,
                MeterCategory as azureMeterCategory,MeterSubcategory as azureMeterSubcategory,MeterId as azureMeterId,
                MeterName as azureMeterName, ResourceType as azureResourceType, ServiceTier as azureServiceTier,
                InstanceId as azureInstanceId, ResourceLocation as region,  ResourceGroup as azureResourceGroup,
                SubscriptionGuid as azureSubscriptionGuid, ServiceName as azureServiceName,
                "AZURE" AS cloudProvider, `%s.CE_INTERNAL.jsonStringToLabelsStruct`(Tags) as labels
           FROM `%s.azureBilling_%s` ;
     """ % (ds, date_start, date_end, ds, jsonData["projectName"], ds, jsonData["tableSuffix"])
    #print(query)
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

def createUDF(client, projectId):
    create_dataset(client, "CE_INTERNAL")
    query = """CREATE FUNCTION IF NOT EXISTS `%s.CE_INTERNAL.jsonStringToLabelsStruct`(input STRING)
                RETURNS Array<STRUCT<key String, value String>>
                LANGUAGE js AS \"""
                var output = []
                if(!input || input.length === 0) {
                    return output;
                }
                var data = JSON.parse(input);
                for (const [key, value] of Object.entries(data)) {
                    newobj = {};
                    newobj.key = key;
                    newobj.value = value;
                    output.push(newobj);
                };
                return output;
                \""";
    """ % (projectId)

    query_job = client.query(query)
    query_job.result()
