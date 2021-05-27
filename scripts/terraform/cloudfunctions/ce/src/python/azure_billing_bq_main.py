"""
CF2 for Azure data pipeline
"""
import json
import base64
import os
import re
from google.cloud import bigquery
from google.cloud import storage
import datetime
import util
from util import create_dataset, if_tbl_exists, createTable, print_, TABLE_NAME_FORMAT
from calendar import monthrange

"""
Event format:
{
	"bucket": "azurecustomerbillingdata-qa",
	"path": "kmpySmUISimoRrJL6NL73w/JUKVZIGKQzCVKXYbDhmM_g/20210101-20210131/cereportnikunj_0e8fa366-608d-4aa8-8fb7-dc8fef9fe717.csv",
	"accountId": "kmpysmuisimorrjl6nl73w",
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

    # Set the accountId for GCP logging
    util.ACCOUNTID_LOG = jsonData.get("accountId")

    # path is folder name in this format vZYBQdFRSlesqo3CMB90Ag/myqO-niJS46aVm3b646SKA/cereportnikunj/20210201-20210228/
    # or in  vZYBQdFRSlesqo3CMB90Ag/myqO-niJS46aVm3b646SKA/<tenantid>/cereportnikunj/20210201-20210228/
    ps = jsonData["path"].split("/")
    if len(ps) not in [4, 5]:
        raise Exception("Invalid path format %s" % jsonData["path"])
    else:
        if len(ps) == 4:
            # old flow
            jsonData["tenant_id"] = ""
        elif len(ps) == 5:
            # TODO: IMP. Check this in new flow
            jsonData["tenant_id"] = ps[2]

    monthfolder = ps[-1]  # last folder in path
    reportYear = monthfolder.split("-")[0][:4]
    reportMonth = monthfolder.split("-")[0][4:6]

    connector_id = ps[1]  # second from beginning is connector id in mongo

    accountIdBQ = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())
    jsonData["datasetName"] = "BillingReport_%s" % (accountIdBQ)

    jsonData["tableSuffix"] = "%s_%s_%s" % (reportYear, reportMonth, connector_id)
    jsonData["tableName"] = f"azureBilling_{jsonData['tableSuffix']}"
    jsonData["tableId"] = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], jsonData["tableName"])

    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    preAggragatedTableRef = dataset.table("preAggregated")
    preAggregatedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.%s.%s" % (PROJECTID, jsonData["datasetName"], "unifiedTable")

    if not if_tbl_exists(client, unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableTableName)
    else:
        alter_unified_table(jsonData)
        print_("%s table exists" % unifiedTableTableName)

    if not if_tbl_exists(client, preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggregatedTableTableName)
    else:
        alter_preagg_table(jsonData)
        print_("%s table exists" % preAggregatedTableTableName)

    # start streaming the data from the gcs
    print_("%s table exists. Starting to write data from gcs into it..." % jsonData["tableName"])
    try:
        ingest_data_from_csv(jsonData)
    except Exception as e:
        print_(e)
        raise
    azure_column_mapping = setAvailableColumns(jsonData)
    get_unique_subs_id(jsonData, azure_column_mapping)
    ingest_data_into_preagg(jsonData, azure_column_mapping)
    ingest_data_into_unified(jsonData, azure_column_mapping)


def ingest_data_from_csv(jsonData):
    # Determine blob of highest size in this folder
    # TODO: This can be moved to CF1
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix=jsonData["path"]
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
    uri = "gs://" + jsonData["bucket"] + "/" + csvtoingest
    print_("Ingesting CSV from %s" % uri)
    print_("Loading into %s table..." % jsonData["tableId"])
    load_job = client.load_table_from_uri(
        [uri],
        jsonData["tableId"],
        job_config=job_config
    )

    load_job.result()  # Wait for the job to complete.
    table = client.get_table(jsonData["tableId"])
    print_("Total {} rows in table {}".format(table.num_rows, jsonData["tableId"]))
    jsonData["uri"] = uri

    # cleanup the processed blobs
    blobs = storage_client.list_blobs(
        jsonData["bucket"], prefix=jsonData["path"]
    )
    for blob in blobs:
        if blob.name.endswith(".csv") or blob.name.endswith(".csv.gz"):
            blob.delete()
            print_("Blob {} deleted.".format(blob.name))


def get_unique_subs_id(jsonData, azure_column_mapping):
    # Get unique subsids from main azureBilling table
    query = """ 
            SELECT DISTINCT(%s) as subscriptionid FROM `%s`;
            """ % (azure_column_mapping["azureSubscriptionGuid"], jsonData["tableId"])
    try:
        query_job = client.query(query)
        results = query_job.result()  # wait for job to complete
        subsids = []
        for row in results:
            subsids.append(row.subscriptionid)
        jsonData["subsId"] = ", ".join(f"'{w}'" for w in subsids)
    except Exception as e:
        print_("Failed to retrieve distinct subsids", "WARN")
        jsonData["subsId"] = ""
        raise e
    print_("Found unique subsids %s" % subsids)


def setAvailableColumns(jsonData):
    azure_column_mapping = {
        "startTime": "",
        "azureResourceRate": "",
        "cost": "",
        "region": "",
        "azureSubscriptionGuid": "",
        "azureInstanceId": "",
    }

    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    query = """SELECT column_name, data_type
            FROM %s.INFORMATION_SCHEMA.COLUMNS
            WHERE table_name="%s";
            """ % (ds, jsonData["tableName"])
    try:
        query_job = client.query(query)
        results = query_job.result()  # wait for job to complete
        columns = set()
        for row in results:
            columns.add(row.column_name)
        jsonData["columns"] = columns
        print_("Retrieved available columns: %s" % columns)
    except Exception as e:
        print_("Failed to retrieve available columns", "WARN")
        jsonData["columns"] = None
        raise e

    # startTime
    if "Date" in columns:
        azure_column_mapping["startTime"] = "Date"
    elif "UsageDateTime" in columns:
        azure_column_mapping["startTime"] = "UsageDateTime"
    else:
        raise Exception("No mapping found for startTime column")

    # azureResourceRate
    if "EffectivePrice" in columns:
        azure_column_mapping["azureResourceRate"] = "EffectivePrice"
    elif "ResourceRate" in columns:
        azure_column_mapping["azureResourceRate"] = "ResourceRate"
    else:
        raise Exception("No mapping found for azureResourceRate column")

    # cost
    if "CostInBillingCurrency" in columns:
        azure_column_mapping["cost"] = "CostInBillingCurrency"
    elif "PreTaxCost" in columns:
        azure_column_mapping["cost"] = "PreTaxCost"
    elif "Cost" in columns:
        azure_column_mapping["cost"] = "Cost"
    else:
        raise Exception("No mapping found for cost column")

    # azureSubscriptionGuid
    if "SubscriptionId" in columns:
        azure_column_mapping["azureSubscriptionGuid"] = "SubscriptionId"
    elif "SubscriptionGuid" in columns:
        azure_column_mapping["azureSubscriptionGuid"] = "SubscriptionGuid"
    else:
        raise Exception("No mapping found for azureSubscriptionGuid column")

    # azureInstanceId
    if "ResourceId" in columns:
        azure_column_mapping["azureInstanceId"] = "ResourceId"
    elif "InstanceId" in columns:
        azure_column_mapping["azureInstanceId"] = "InstanceId"
    else:
        raise Exception("No mapping found for azureInstanceId column")
    print_("azure_column_mapping: %s" % azure_column_mapping)
    return azure_column_mapping


def ingest_data_into_preagg(jsonData, azure_column_mapping):
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "preAggregated")
    year, month, _ = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s preAggregated table..." % tableName)
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s' 
                AND cloudProvider = "AZURE" AND azureSubscriptionGuid IN (%s);
           INSERT INTO `%s.preAggregated` (startTime, azureResourceRate, cost,
                                           azureServiceName, region, azureSubscriptionGuid,
                                            cloudProvider, azureTenantId)
           SELECT TIMESTAMP(%s) as startTime, min(%s) AS azureResourceRate, sum(%s) AS cost,
                MeterCategory AS azureServiceName, ResourceLocation as region, %s as azureSubscriptionGuid,
                "AZURE" AS cloudProvider, '%s' as azureTenantId
           FROM `%s`
           WHERE %s IN (%s)
           GROUP BY azureServiceName, region, azureSubscriptionGuid, startTime;
    """ % (ds, date_start, date_end, jsonData["subsId"], ds, azure_column_mapping["startTime"],
           azure_column_mapping["azureResourceRate"],
           azure_column_mapping["cost"], azure_column_mapping["azureSubscriptionGuid"], jsonData["tenant_id"],
           jsonData["tableId"],
           azure_column_mapping["azureSubscriptionGuid"], jsonData["subsId"])

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
    query_job.result()
    print_("Loaded into %s table..." % tableName)


def ingest_data_into_unified(jsonData, azure_column_mapping):
    create_bq_udf()
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    tableName = "%s.%s" % (ds, "unifiedTable")
    year, month, _ = jsonData["tableSuffix"].split('_')
    date_start = "%s-%s-01" % (year, month)
    date_end = "%s-%s-%s" % (year, month, monthrange(int(year), int(month))[1])
    print_("Loading into %s table..." % tableName)
    # Prepare default columns
    insert_columns = """product, startTime, cost,
                        azureMeterCategory, azureMeterSubcategory, azureMeterId,
                        azureMeterName,
                        azureInstanceId, region, azureResourceGroup,
                        azureSubscriptionGuid, azureServiceName,
                        cloudProvider, labels, azureResource, azureVMProviderId, azureTenantId
                    """
    select_columns = """MeterCategory AS product, TIMESTAMP(%s) as startTime, %s AS cost,
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
                                null)),
                        '%s' as azureTenantId
                     """ % (azure_column_mapping["startTime"],
                            azure_column_mapping["cost"], azure_column_mapping["azureInstanceId"],
                            azure_column_mapping["azureSubscriptionGuid"], PROJECTID,
                            azure_column_mapping["azureInstanceId"],
                            azure_column_mapping["azureInstanceId"], azure_column_mapping["azureInstanceId"],
                            azure_column_mapping["azureInstanceId"],
                            azure_column_mapping["azureInstanceId"], azure_column_mapping["azureInstanceId"],
                            jsonData["tenant_id"])

    # Amend query as per columns availability
    for additionalColumn in ["AccountName", "Frequency", "PublisherType", "ServiceTier", "ResourceType",
                             "SubscriptionName", "ReservationId", "ReservationName", "PublisherName"]:
        if additionalColumn in jsonData["columns"]:
            insert_columns = insert_columns + ", azure%s" % additionalColumn
            select_columns = select_columns + ", %s as azure%s" % (additionalColumn, additionalColumn)

    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= '%s' AND DATE(startTime) <= '%s'  
                AND cloudProvider = "AZURE" AND azureSubscriptionGuid IN (%s);
               INSERT INTO `%s.unifiedTable` (%s)
               SELECT %s
               FROM `%s` 
               WHERE %s IN (%s);
            """ % (
        ds, date_start, date_end, jsonData["subsId"], ds, insert_columns, select_columns, jsonData["tableId"],
        azure_column_mapping["azureSubscriptionGuid"], jsonData["subsId"])

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


def create_bq_udf():
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
    """ % PROJECTID

    query_job = client.query(query)
    query_job.result()


def alter_preagg_table(jsonData):
    print_("Altering Preaggregated Data Table")
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
    query = "ALTER TABLE `%s.preAggregated` \
            ADD COLUMN IF NOT EXISTS azureSubscriptionGuid STRING, \
            ADD COLUMN IF NOT EXISTS azureResourceRate FLOAT64, \
            ADD COLUMN IF NOT EXISTS azureServiceName STRING, \
            ADD COLUMN IF NOT EXISTS azureTenantId STRING;" % ds

    try:
        query_job = client.query(query)
        query_job.result()
    except Exception as e:
        # Error Running Alter Query
        print_(e)
    else:
        print_("Finished Altering preAggregated Table")


def alter_unified_table(jsonData):
    print_("Altering unifiedTable Table")
    ds = "%s.%s" % (PROJECTID, jsonData["datasetName"])
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
        ADD COLUMN IF NOT EXISTS azureResource STRING, \
        ADD COLUMN IF NOT EXISTS azureTenantId STRING;" % ds

    try:
        query_job = client.query(query)
        query_job.result()
    except Exception as e:
        # Error Running Alter Query
        print_(e)
    else:
        print_("Finished Altering unifiedTable Table")
