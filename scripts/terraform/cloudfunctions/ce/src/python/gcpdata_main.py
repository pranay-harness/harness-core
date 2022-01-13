# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import base64
import json
import os
from google.cloud import bigquery
import datetime
import util
from util import create_dataset, if_tbl_exists, createTable, print_

"""
This CF is used in Current Gen

This is for BQ Transfer
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
This is for gcpCopy
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

This is for runOnce
{
    'dataSourceId': 'scheduled_query',
    'destinationDatasetId': 'BillingReport_kmpysmuisimorrjl6nl73w',
    'emailPreferences': {},
    'endTime': '2020-12-14T05:02:38.184608Z',
    'errorStatus': {},
    'name': 'projects/199539700734/locations/us/transferConfigs/602c78da-0000-223e-b3da-001a1143e774/runs/601a7c48-0000-2da3-b213-3c286d4185fe',
    'notificationPubsubTopic': 'projects/ccm-play/topics/ce-gcpdata',
    'params': {
        'destination_table_name_template': 'gcp_billing_export',
        'query': 'SELECT * FROM `ce-qa-274307.BillingReport_zeaak_fls425ieo7olzmug.gcp_billing_export_*`;',
        'write_disposition': 'WRITE_TRUNCATE'
    },
    'runTime': '2020-12-14T05:00:06Z',
    'scheduleTime': '2020-12-14T05:00:07.738675Z',
    'startTime': '2020-12-14T05:00:21.310472Z',
    'state': 'SUCCEEDED',
    'updateTime': '2020-12-14T05:02:38.184619Z',
    'userId': '6106063769767823872'
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

    if jsonData["dataSourceId"] == "scheduled_query" and jsonData.get("destinationDatasetId"):
        # runOnce query completion event
        jsonData["query_type"] = "runOnce"
    elif jsonData["dataSourceId"] == "scheduled_query" and not jsonData["destinationDatasetId"] :
        jsonData["query_type"] = "gcpCopy"
        try:
            jsonData["destinationDatasetId"] = jsonData.get("params", {}).get("query", "").split(".")[1]
        except Exception as e:
            print("Dataset could not be found. %s" % e)
            raise
    elif jsonData["dataSourceId"] == "cross_region_copy":
        jsonData["query_type"] = "BigQueryCopy"

    jsonData["accountId"] = jsonData["destinationDatasetId"].split("BillingReport_")[-1]
    util.ACCOUNTID_LOG = jsonData["accountId"]
    if jsonData["accountId"] in os.environ.get("disable_for_accounts", "").split(","):
        print_("Execution disabled for this account :%s" % jsonData["accountId"])
        return
    state = jsonData["state"] # SUCCEEDED
    jsonData["datasetName"] = jsonData["destinationDatasetId"] # for compatibility
    print_(jsonData)
    client = bigquery.Client(jsonData["projectName"])
    dataset = client.dataset(jsonData["destinationDatasetId"])
    preAggragatedTableRef = dataset.table("preAggregated")
    preAggragatedTableTableName = "%s.BillingReport_%s.%s" % (jsonData["projectName"], jsonData["accountId"], "preAggregated")
    unifiedTableRef = dataset.table("unifiedTable")
    unifiedTableTableName = "%s.BillingReport_%s.%s" % (jsonData["projectName"], jsonData["accountId"], "unifiedTable")
    #create_dataset(client, jsonData)

    create_dataset(client, jsonData["datasetName"])
    if not if_tbl_exists(client, preAggragatedTableRef):
        print_("%s table does not exists, creating table..." % preAggragatedTableRef)
        createTable(client, preAggragatedTableRef)
    else:
        print_("%s table exists" % preAggragatedTableTableName)

    if not if_tbl_exists(client, unifiedTableRef):
        print_("%s table does not exists, creating table..." % unifiedTableRef)
        createTable(client, unifiedTableRef)
    else:
        print_("%s table exists" % unifiedTableTableName)

    if state == "SUCCEEDED":
        loadIntoPreaggregated(client, jsonData)
        loadIntoUnified(client, jsonData)


def loadIntoUnified(client, jsonData):
    if jsonData["query_type"] in ["runOnce", "BigQueryCopy"]:
        INTERVAL = '45'
    else:
        INTERVAL = '3'
    query = """DELETE FROM `%s.unifiedTable` WHERE DATE(startTime) >= DATE_SUB(@run_date , INTERVAL %s DAY) AND cloudProvider = "GCP";
           INSERT INTO `%s.unifiedTable` (product, cost, gcpProduct,gcpSkuId,gcpSkuDescription, startTime, gcpProjectId,
                region,zone,gcpBillingAccountId,cloudProvider, discount, labels)
                SELECT service.description AS product, cost AS cost, service.description AS gcpProduct, sku.id AS gcpSkuId,
                     sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, project.id AS gcpProjectId,
                     location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, "GCP" AS cloudProvider, credits.amount as discount, labels AS labels
                FROM `%s.gcp_billing_export*` LEFT JOIN UNNEST(credits) as credits
                WHERE DATE(usage_start_time) <= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) AND
                     DATE(usage_start_time) >= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL %s DAY) ;
    """ % (jsonData["datasetName"], INTERVAL, jsonData["datasetName"], jsonData["datasetName"], INTERVAL)

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
    print_("Loaded into unifiedTable table...")


def loadIntoPreaggregated(client, jsonData):
    if jsonData["query_type"] in ["runOnce", "BigQueryCopy"]:
        INTERVAL = '45'
    else:
        INTERVAL = '3'
    query = """DELETE FROM `%s.preAggregated` WHERE DATE(startTime) >= DATE_SUB(@run_date , INTERVAL %s DAY) AND cloudProvider = "GCP" ;
           INSERT INTO `%s.preAggregated` (cost, gcpProduct,gcpSkuId,gcpSkuDescription,
             startTime,gcpProjectId,region,zone,gcpBillingAccountId,cloudProvider, discount) SELECT SUM(cost) AS cost, service.description AS gcpProduct,
             sku.id AS gcpSkuId, sku.description AS gcpSkuDescription, TIMESTAMP_TRUNC(usage_start_time, DAY) as startTime, project.id AS gcpProjectId,
             location.region AS region, location.zone AS zone, billing_account_id AS gcpBillingAccountId, "GCP" AS cloudProvider, SUM(credits.amount) as
             discount
           FROM `%s.gcp_billing_export*` LEFT JOIN UNNEST(credits) as credits
           WHERE DATE(usage_start_time) <= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL 1 DAY) AND
             DATE(usage_start_time) >= DATE_SUB(CAST(FORMAT_DATE('%%Y-%%m-%%d', @run_date) AS DATE), INTERVAL %s DAY)
           GROUP BY service.description, sku.id, sku.description, startTime, project.id, location.region, location.zone, billing_account_id;
    """ % (jsonData["datasetName"], INTERVAL, jsonData["datasetName"], jsonData["datasetName"], INTERVAL)

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
    print_("Loaded into preAggregated table...")
