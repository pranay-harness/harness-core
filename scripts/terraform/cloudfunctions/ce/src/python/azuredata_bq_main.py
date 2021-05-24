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

CE_COLUMN_MAPPING = {
    "startTime": "",
    "azureResourceRate": "",
    "cost": "",
    "region": "",
    "azureSubscriptionGuid": "",
    "azureInstanceId": "",
}


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
        alterUnifiedTable(client, jsonData)
        print_("%s table exists" % unifiedTableTableName)

    if not if_tbl_exists(client, preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggragatedTableTableName)
    else:
        alterPreaggTable(client, jsonData)
        print_("%s table exists" % preAggragatedTableTableName)

    # start streaming the data from the gcs
    print_("%s table exists. Starting to write data from gcs into it..." % jsonData["tableName"])
    try:
        ingestDataFromCsvToAzureTable(client, jsonData)
    except Exception as e:
        print_(e, "WARN")
        return
    setAvailableColumns(client, jsonData)
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
        write_disposition='WRITE_TRUNCATE'  # If the table already exists, BigQuery overwrites the table data
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
            print_("Blob {} deleted.".format(blob.name))


def setAvailableColumns(client, jsonData):
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    query = """SELECT column_name, data_type
            FROM %s.INFORMATION_SCHEMA.COLUMNS
            WHERE table_name="azureBilling_%s";
            """ % (ds, jsonData["tableSuffix"])
    try:
        query_job = client.query(query)
        results = query_job.result()  # wait for job to complete
        columns = set()
        for row in results:
            columns.add(row.column_name)
        jsonData["columns"] = columns
        print_("Retrieved available columns")
    except Exception as e:
        print_("Failed to retrieve available columns", "WARN")
        jsonData["columns"] = None
        raise e

    # startTime
    if "Date" in columns:
        CE_COLUMN_MAPPING["startTime"] = "Date"
    elif "UsageDateTime" in columns:
        CE_COLUMN_MAPPING["startTime"] = "UsageDateTime"
    else:
        raise Exception("No mapping found for startTime column")

    # azureResourceRate
    if "EffectivePrice" in columns:
        CE_COLUMN_MAPPING["azureResourceRate"] = "EffectivePrice"
    elif "ResourceRate" in columns:
        CE_COLUMN_MAPPING["azureResourceRate"] = "ResourceRate"
    else:
        raise Exception("No mapping found for azureResourceRate column")

    # cost
    if "CostInBillingCurrency" in columns:
        CE_COLUMN_MAPPING["cost"] = "CostInBillingCurrency"
    elif "PreTaxCost" in columns:
        CE_COLUMN_MAPPING["cost"] = "PreTaxCost"
    elif "Cost" in columns:
        CE_COLUMN_MAPPING["cost"] = "Cost"
    else:
        raise Exception("No mapping found for cost column")

    # azureSubscriptionGuid
    if "SubscriptionId" in columns:
        CE_COLUMN_MAPPING["azureSubscriptionGuid"] = "SubscriptionId"
    elif "SubscriptionGuid" in columns:
        CE_COLUMN_MAPPING["azureSubscriptionGuid"] = "SubscriptionGuid"
    else:
        raise Exception("No mapping found for azureSubscriptionGuid column")

    # azureInstanceId
    if "ResourceId" in columns:
        CE_COLUMN_MAPPING["azureInstanceId"] = "ResourceId"
    elif "InstanceId" in columns:
        CE_COLUMN_MAPPING["azureInstanceId"] = "InstanceId"
    else:
        raise Exception("No mapping found for azureInstanceId column")
    print_(CE_COLUMN_MAPPING)


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
           SELECT TIMESTAMP(%s) as startTime, min(%s) AS azureResourceRate, sum(%s) AS cost,
                MeterCategory AS azureServiceName, ResourceLocation as region, %s as azureSubscriptionGuid,
                "AZURE" AS cloudProvider
           FROM `%s.azureBilling_%s`
           GROUP BY azureServiceName, region, azureSubscriptionGuid, startTime;
    """ % (ds, date_start, date_end, ds, CE_COLUMN_MAPPING["startTime"], CE_COLUMN_MAPPING["azureResourceRate"],
           CE_COLUMN_MAPPING["cost"], CE_COLUMN_MAPPING["azureSubscriptionGuid"], ds, jsonData["tableSuffix"])

    # print(query)
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
    # Prepare default columns
    INSERT_COLUMNS = """product, startTime, cost,
                        azureMeterCategory, azureMeterSubcategory, azureMeterId,
                        azureMeterName,
                        azureInstanceId, region, azureResourceGroup,
                        azureSubscriptionGuid, azureServiceName,
                        cloudProvider, labels, azureResource, azureVMProviderId"""
    SELECT_COLUMNS = """MeterCategory AS product, TIMESTAMP(%s) as startTime, %s AS cost,
                        MeterCategory as azureMeterCategory,MeterSubcategory as azureMeterSubcategory,MeterId as azureMeterId,
                        MeterName as azureMeterName,
                        %s as azureInstanceId, ResourceLocation as region,  ResourceGroup as azureResourceGroup,
                        %s as azureSubscriptionGuid, MeterCategory as azureServiceName,
                        "AZURE" AS cloudProvider, `%s.CE_INTERNAL.jsonStringToLabelsStruct`(Tags) as labels,
                        ARRAY_REVERSE(SPLIT(%s,REGEXP_EXTRACT(%s, r'(?i)providers/')))[OFFSET(0)] as azureResource,
                        IF(REGEXP_CONTAINS(%s, r'virtualMachineScaleSets'),
                            LOWER(CONCAT('azure://', %s, '/virtualMachines/',
                                REGEXP_EXTRACT(JSON_VALUE(AdditionalInfo, '$.VMName'), r'_([0-9]+)$') )),
                            IF(REGEXP_CONTAINS(%s, r'virtualMachines'),
                                LOWER(CONCAT('azure://', %s)),
                                null))
                     """ % (CE_COLUMN_MAPPING["startTime"],
                            CE_COLUMN_MAPPING["cost"], CE_COLUMN_MAPPING["azureInstanceId"],
                            CE_COLUMN_MAPPING["azureSubscriptionGuid"], jsonData["projectName"],
                            CE_COLUMN_MAPPING["azureInstanceId"],
                            CE_COLUMN_MAPPING["azureInstanceId"], CE_COLUMN_MAPPING["azureInstanceId"],
                            CE_COLUMN_MAPPING["azureInstanceId"],
                            CE_COLUMN_MAPPING["azureInstanceId"], CE_COLUMN_MAPPING["azureInstanceId"])

    # Amend query as per columns availability
    for additionalColumn in ["AccountName", "Frequency", "PublisherType", "ServiceTier", "ResourceType",
                             "SubscriptionName", "ReservationId", "ReservationName", "PublisherName"]:
        if additionalColumn in jsonData["columns"]:
            INSERT_COLUMNS = INSERT_COLUMNS + ", azure%s" % additionalColumn
            SELECT_COLUMNS = SELECT_COLUMNS + ", %s as azure%s" % (additionalColumn, additionalColumn)

    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s'  AND cloudProvider = "AZURE";
               INSERT INTO `%s.unifiedTable` (%s)
               SELECT %s
               FROM `%s.azureBilling_%s` ;
            """ % (ds, date_start, date_end, ds, INSERT_COLUMNS, SELECT_COLUMNS, ds, jsonData["tableSuffix"])
    print_(query)
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
                try {
                    var data = JSON.parse(input);
                } catch (SyntaxError) {
                    input="{".concat(input, "}")
                    try {
                        var data = JSON.parse(input);
                    } catch (SyntaxError) {
                        return output;
                    }
                }
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


def alterPreaggTable(client, jsonData):
    print_("Altering Preaggregated Data Table")
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    query = "ALTER TABLE `%s.preAggregated` ADD COLUMN IF NOT EXISTS azureSubscriptionGuid STRING, \
        ADD COLUMN IF NOT EXISTS azureResourceRate FLOAT64, \
        ADD COLUMN IF NOT EXISTS azureServiceName STRING;" % (ds)

    try:
        query_job = client.query(query)
        results = query_job.result()
    except Exception as e:
        # Error Running Alter Query
        print_(e)
    else:
        print_("Finished Altering preAggregated Table")


def alterUnifiedTable(client, jsonData):
    print_("Altering unifiedTable Table")
    ds = "%s.%s" % (jsonData["projectName"], jsonData["datasetName"])
    query = "ALTER TABLE `%s.unifiedTable` ADD COLUMN IF NOT EXISTS azureMeterCategory STRING, \
        ADD COLUMN IF NOT EXISTS azureMeterSubcategory STRING, \
        ADD COLUMN IF NOT EXISTS azureMeterId STRING, \
        ADD COLUMN IF NOT EXISTS azureMeterName STRING, \
        ADD COLUMN IF NOT EXISTS azureResourceType STRING, \
        ADD COLUMN IF NOT EXISTS azureServiceTier STRING, \
        ADD COLUMN IF NOT EXISTS azureInstanceId STRING, \
        ADD COLUMN IF NOT EXISTS azureResourceGroup STRING, \
        ADD COLUMN IF NOT EXISTS azureSubscriptionGuid STRING, \
        ADD COLUMN IF NOT EXISTS azureAccountName STRING, \
        ADD COLUMN IF NOT EXISTS azureFrequency STRING, \
        ADD COLUMN IF NOT EXISTS azurePublisherType STRING, \
        ADD COLUMN IF NOT EXISTS azureSubscriptionName STRING, \
        ADD COLUMN IF NOT EXISTS azureReservationId STRING, \
        ADD COLUMN IF NOT EXISTS azureReservationName STRING, \
        ADD COLUMN IF NOT EXISTS azurePublisherName STRING, \
        ADD COLUMN IF NOT EXISTS azureServiceName STRING, \
        ADD COLUMN IF NOT EXISTS azureVMProviderId STRING, \
        ADD COLUMN IF NOT EXISTS azureResource STRING;" % ds

    try:
        query_job = client.query(query)
        results = query_job.result()
    except Exception as e:
        # Error Running Alter Query
        print_(e)
    else:
        print_("Finished Altering unifiedTable Table")
