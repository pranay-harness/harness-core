import base64
import json
import os
from google.cloud import bigquery
import datetime
from util import create_dataset, if_tbl_exists, createTable

"""
{
	'dataSourceId': 'cross_region_copy',
	'destinationDatasetId': 'BillingReport_zeaak_fls425ieo7olzmug',
	'emailPreferences': {},
	'endTime': '2020-11-15T15:19:49.161214Z',
	'errorStatus': {},
	'name': 'projects/622361681186/locations/us/transferConfigs/5f79f4b9-0000-2950-8cc5-089e0825ee3c/runs/6075aaf4-0000-26f5-a6b0-94eb2c09d0a8',
	'notificationPubsubTopic': 'projects/ce-qa-274307/topics/ce-gcp-dataset-copy-topic',
	'params': {
		'overwrite_destination_table': True,
		'source_dataset_id': 'billing_prod_all_projects',
		'source_project_id': 'prod-setup-205416'
	},
	'runTime': '2020-11-15T15:17:44.113Z',
	'scheduleTime': '2020-11-15T15:17:48.877574Z',
	'state': 'SUCCEEDED',
	'updateTime': '2020-11-15T15:19:49.161227Z',
	'userId': '-7372399484122590985'
}

{
	'dataSourceId': 'scheduled_query',
	'destinationDatasetId': '',
	'emailPreferences': {},
	'endTime': '2020-11-16T10:23:38.948114Z',
	'errorStatus': {},
	'name': 'projects/622361681186/locations/us/transferConfigs/5f6e563d-0000-26b4-bc76-001a11450baa/runs/5fedbdc3-0000-24e6-b75a-f403045e6250',
	'notificationPubsubTopic': 'projects/ce-qa-274307/topics/ce-gcp-dataset-copy-topic',
	'params': {
		'query': 'DELETE FROM `ce-qa-274307.BillingReport_ujs3j8rdttu4llzxiui6sg.gcp_billing_export` WHERE DATE(usage_start_time) >= DATE_SUB(@run_date , INTERVAL 3 DAY);\nINSERT INTO `ce-qa-274307.BillingReport_ujs3j8rdttu4llzxiui6sg.gcp_billing_export` SELECT * FROM `ccm-play.billing.gcp_billing_export_*` WHERE DATE(usage_start_time) >= DATE_SUB(@run_date , INTERVAL 3 DAY);'
	},
	'runTime': '2020-11-16T02:00:00Z',
	'scheduleTime': '2020-11-16T10:20:38.510647Z',
	'startTime': '2020-11-16T10:20:38.76202Z',
	'state': 'SUCCEEDED',
	'updateTime': '2020-11-16T10:23:38.948131Z',
	'userId': '6656494147196134907'
}
"""

def main(event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
      event (dict): Event payload.
      context (google.cloud.functions.Context): Metadata for the event.
    """
    pubsub_message = base64.b64decode(event['data']).decode('utf-8')
    jsonData = json.loads(pubsub_message)
    print("event json: %s" % jsonData)
    print("context: %s" % context)
    if os.environ.get('disabled', 'false').lower() == 'true':
        print("Function is disabled...")
        return
    jsonData["projectName"] = os.environ.get('GCP_PROJECT', 'ce-prod-274307')
    if jsonData["dataSourceId"] == "scheduled_query":
        # dataset needs to be fetched from params.query
        try:
            jsonData["destinationDatasetId"] = jsonData.get("params", {}).get("query", "").split(".")[1]
        except:
            print("Dataset could not be found.. returning")
    # BillingReport_zeaak_fls425ieo7olzmug
    jsonData["accountId"] = jsonData["destinationDatasetId"].split("BillingReport_")[-1]
    if jsonData["accountId"] in os.environ.get("disable_for_accounts", "").split(","):
        print("Execution disabled for this account :%s" % jsonData["accountId"])
        return
    state = jsonData["state"] # SUCCEEDED
    jsonData["datasetName"] = jsonData["destinationDatasetId"] # for compatibility
    print(jsonData)
    client = bigquery.Client(jsonData["projectName"])
    dataset = client.dataset(jsonData["destinationDatasetId"])
    preAggragatedTableRef = dataset.table("preAggregated")
    preAggragatedTableTableName = "%s.BillingReport_%s.%s" % (jsonData["projectName"], jsonData["accountId"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.BillingReport_%s.%s" % (jsonData["projectName"], jsonData["accountId"], "unifiedTable")
    create_dataset(client, jsonData)
    if not if_tbl_exists(client, preAggragatedTableRef):
        print("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggragatedTableTableName)
    else:
        print("%s table exists" % preAggragatedTableTableName)

    if not if_tbl_exists(client, unifiedTableRef):
        print("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableTableName)
    else:
        print("%s table exists" % unifiedTableTableName)

    if state == "SUCCEEDED":
        loadIntoPreaggregated(client, jsonData)
        loadIntoUnified(client, jsonData)


def loadIntoUnified(client, jsonData):
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= DATE_SUB(@run_date , INTERVAL 3 DAY) AND cloudProvider = "GCP";
           INSERT INTO `%s.unifiedTable` (product, cost, gcpProduct,gcpSkuId,gcpSkuDescription, startTime, gcpProjectId,
                region,zone,gcpBillingAccountId,cloudProvider, discount, labels)
                SELECT service.description AS product, cost AS cost, service.description AS gcpProduct, sku.id AS gcpSkuId,
                     sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, project.id AS gcpProjectId,
                     location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, "GCP" AS cloudProvider, credits.amount as discount, labels AS labels
                FROM `%s.gcp_billing_export*` LEFT JOIN UNNEST(credits) as credits
                WHERE DATE(usage_start_time) <= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) AND
                     DATE(usage_start_time) >= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 3 DAY) ;
    """ % (jsonData["datasetName"], jsonData["datasetName"], jsonData["datasetName"])

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
    print("Loaded into unifiedTable table...")


def loadIntoPreaggregated(client, jsonData):
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= DATE_SUB(@run_date , INTERVAL 3 DAY) AND cloudProvider = "GCP";
           INSERT INTO `%s.preAggregated` (cost, gcpProduct,gcpSkuId,gcpSkuDescription,
             startTime,gcpProjectId,region,zone,gcpBillingAccountId,cloudProvider, discount) SELECT SUM(cost) AS cost, service.description AS gcpProduct,
             sku.id AS gcpSkuId, sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, project.id AS gcpProjectId,
             location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, "GCP" AS cloudProvider, SUM(credits.amount) as
             discount
           FROM `%s.gcp_billing_export*` LEFT JOIN UNNEST(credits) as credits
           WHERE DATE(usage_start_time) <= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) AND
             DATE(usage_start_time) >= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 3 DAY)
           GROUP BY service.description, sku.id, sku.description, startTime, project.id, location.region, location.zone, billing_account_id;
    """ % (jsonData["datasetName"], jsonData["datasetName"], jsonData["datasetName"])

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
    print("Loaded into preAggregated table...")