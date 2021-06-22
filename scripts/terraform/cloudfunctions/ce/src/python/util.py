import json
from google.cloud import bigquery
from google.cloud.exceptions import NotFound
from bq_schema import awsEc2InventorySchema, awsEc2InventoryCPUSchema, preAggreagtedTableSchema, \
    unifiedTableTableSchema, clusterDataTableFields, awsEbsInventoryMetricsSchema, awsEbsInventorySchema, \
    aws_cur_table_schema

ACCOUNTID_LOG = ""
TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"

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
    if tableName == "clusterData":
        fieldset = clusterDataTableFields
        partition = bigquery.RangePartitioning(
            field="starttime",
            range_=bigquery.PartitionRange(start=1514745000000, end=1893436200000, interval=86400000)
        )
    elif tableName == "preAggregated":
        fieldset = preAggreagtedTableSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="startTime"
        )
    elif tableName == "awsEc2InventoryMetric":
        fieldset = awsEc2InventoryCPUSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="addedAt"
        )
    elif tableName.startswith("awsEc2Inventory"):
        fieldset = awsEc2InventorySchema
        partition = bigquery.RangePartitioning(
            range_=bigquery.PartitionRange(start=0, end=10000, interval=1),
            field="linkedAccountIdPartition"
        )
    elif tableName == "awsEbsInventoryMetrics":
        fieldset = awsEbsInventoryMetricsSchema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="addedAt"
        )
    elif tableName.startswith("awsEbsInventory"):
        fieldset = awsEbsInventorySchema
        partition = bigquery.RangePartitioning(
            range_=bigquery.PartitionRange(start=0, end=10000, interval=1),
            field="linkedAccountIdPartition"
        )
    elif tableName.startswith("awscur"):
        fieldset = aws_cur_table_schema
        partition = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="usagestartdate"
        )
    else:
        fieldset = unifiedTableTableSchema
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

    if tableName == "unifiedTable" or tableName == "preAggregated" or \
        tableName == "awsEc2InventoryMetric" or tableName == "awsEbsInventoryMetrics" or \
        tableName.startswith("awscur"):
        table.time_partitioning = partition
    elif tableName.startswith("awsEc2Inventory") or tableName.startswith("awsEbsInventory") or tableName == "clusterData":
        table.range_partitioning = partition

    try:
        table = client.create_table(table)  # Make an API request.
        print_("Created table {}.{}.{}".format(table.project, table.dataset_id, table.table_id))
    except Exception as e:
        print_("Error while creating table\n {}".format(e), "WARN")
        return False
    return True
