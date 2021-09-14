# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import json
import bq_schema
from google.cloud import bigquery
from google.cloud.exceptions import NotFound

ACCOUNTID_LOG = ""
TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"

# TABLE NAMES
CLUSTERDATAAGGREGATED = "clusterDataAggregated"
CLUSTERDATA = "clusterData"
CLUSTERDATAHOURLY = "clusterDataHourly"
CLUSTERDATAHOURLYAGGREGATED = "clusterDataHourlyAggregated"
PREAGGREGATED = "preAggregated"
AWSEC2INVENTORYMETRIC = "awsEc2InventoryMetric"
AWSEC2INVENTORY = "awsEc2Inventory"
AWSEBSINVENTORYMETRICS = "awsEbsInventoryMetrics"
AWSEBSINVENTORY = "awsEbsInventory"
UNIFIED = "unifiedTable"
AWSCURPREFIX = "awscur"

def print_(message, severity="INFO"):
    # Set account id in the beginning of your CF call
    try:
        print(json.dumps({"accountId":ACCOUNTID_LOG, "severity":severity, "message": message}))
    except:
        print(message)

def create_dataset(client, datasetName):
    dataset_id = "{}.{}".format(client.project, datasetName)
    dataset = bigquery.Dataset(dataset_id)
    dataset.location = "US"
    dataset.description = "Dataset for [ AccountId: %s ]" % (ACCOUNTID_LOG)

    # Send the dataset to the API for creation, with an explicit timeout.
    # Raises google.api_core.exceptions.Conflict if the Dataset already
    # exists within the project.
    try:
        dataset = client.create_dataset(dataset, timeout=30)  # Make an API request.
        print_("Created dataset {}.{}".format(client.project, dataset.dataset_id))
    except Exception as e:
        print_("Dataset {} already exists {}".format(dataset_id, e), "WARN")


def if_tbl_exists(client, table_ref):
    try:
        client.get_table(table_ref)
        return True
    except NotFound:
        return False


def createTable(client, table_ref):
    tableName = table_ref.table_id
    print_("Creating %s table" % tableName)
    schema = []
    if tableName == CLUSTERDATA or tableName == CLUSTERDATAHOURLY:
        fieldset = bq_schema.clusterDataTableFields
        partition = bigquery.RangePartitioning(
            field="starttime",
            range_=bigquery.PartitionRange(start=1514745000000, end=1893436200000, interval=86400000)
        )
    elif tableName == CLUSTERDATAAGGREGATED or tableName == CLUSTERDATAHOURLYAGGREGATED:
        fieldset = bq_schema.clusterDataAggregatedFields
        partition = bigquery.RangePartitioning(
            field="starttime",
            range_=bigquery.PartitionRange(start=1514745000000, end=1893436200000, interval=86400000)
        )
    elif tableName == PREAGGREGATED:
        fieldset = bq_schema.preAggreagtedTableSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="startTime"
        )
    elif tableName == AWSEC2INVENTORYMETRIC:
        fieldset = bq_schema.awsEc2InventoryCPUSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="addedAt"
        )
    elif tableName.startswith(AWSEC2INVENTORY):
        fieldset = bq_schema.awsEc2InventorySchema
        partition = bigquery.RangePartitioning(
            range_=bigquery.PartitionRange(start=0, end=10000, interval=1),
            field="linkedAccountIdPartition"
        )
    elif tableName == AWSEBSINVENTORYMETRICS:
        fieldset = bq_schema.awsEbsInventoryMetricsSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="addedAt"
        )
    elif tableName.startswith(AWSEBSINVENTORY):
        fieldset = bq_schema.awsEbsInventorySchema
        partition = bigquery.RangePartitioning(
            range_=bigquery.PartitionRange(start=0, end=10000, interval=1),
            field="linkedAccountIdPartition"
        )
    elif tableName.startswith(AWSCURPREFIX):
        fieldset = bq_schema.aws_cur_table_schema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="usagestartdate"
        )
    elif tableName == UNIFIED:
        fieldset = bq_schema.unifiedTableTableSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="startTime"  # name of column to use for partitioning
        )

    for field in fieldset:
        if field.get("type") == "RECORD":
            nested_field = [bigquery.SchemaField(nested_field["name"], nested_field["type"], mode=nested_field.get("mode", "")) for nested_field in field["fields"]]
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field["mode"], fields=nested_field))
        else:
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field.get("mode", "")))
    if not schema:
        print_("Could not find any schema for table %s : %s" % (tableName, schema))
        return False
    table = bigquery.Table("%s.%s.%s" % (table_ref.project, table_ref.dataset_id, tableName), schema=schema)

    if tableName in [UNIFIED, PREAGGREGATED, AWSEC2INVENTORYMETRIC, AWSEBSINVENTORYMETRICS] or \
        tableName.startswith(AWSCURPREFIX):
        table.time_partitioning = partition
    elif tableName.startswith(AWSEC2INVENTORY) or tableName.startswith(AWSEBSINVENTORY) or \
            tableName in [CLUSTERDATA, CLUSTERDATAAGGREGATED, CLUSTERDATAHOURLY, CLUSTERDATAHOURLYAGGREGATED]:
        table.range_partitioning = partition

    try:
        table = client.create_table(table)  # Make an API request.
        print_("Created table {}.{}.{}".format(table.project, table.dataset_id, table.table_id))
    except Exception as e:
        print_("Error while creating table\n {}".format(e), "WARN")
        return False
    return True
