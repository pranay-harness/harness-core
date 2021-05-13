import json
from google.cloud import bigquery
from google.cloud.exceptions import NotFound
from clusterdata_schema import clusterDataTableFields
from unified_schema import unifiedTableTableSchema
from preaggregated_schema import preAggreagtedTableSchema
from aws_ec2_inventory_schema import awsEc2InventorySchema, awsEc2InventoryCPUSchema
from aws_ebs_inventory_schema import awsEbsInventorySchema, awsEbsInventoryMetricsSchema

ACCOUNTID_LOG = ""

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
    dataset.description = "Data set for [ AccountId: %s ]" % (ACCOUNTID_LOG)

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


def createTable(client, tableName):
    print_("Creating %s table" % tableName)
    schema = []
    if tableName.endswith("clusterData"):
        fieldset = clusterDataTableFields
    elif tableName.endswith("preAggregated"):
        fieldset = preAggreagtedTableSchema
    elif tableName.endswith("awsEc2Inventory") or tableName.endswith("awsEc2InventoryTemp"):
        fieldset = awsEc2InventorySchema
    elif tableName.endswith("awsEc2InventoryCPU"):
        fieldset = awsEc2InventoryCPUSchema
    elif tableName.endswith("awsEbsInventory") or tableName.endswith("awsEbsInventoryTemp"):
        fieldset = awsEbsInventorySchema
    elif tableName.endswith("awsEbsInventoryMetrics"):
        fieldset = awsEbsInventoryMetricsSchema
    else:
        fieldset = unifiedTableTableSchema

    for field in fieldset:
        if field.get("type") == "RECORD":
            nested_field = [bigquery.SchemaField(nested_field["name"], nested_field["type"], mode=nested_field.get("mode", "")) for nested_field in field["fields"]]
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field["mode"], fields=nested_field))
        else:
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field.get("mode", "")))
    table = bigquery.Table(tableName, schema=schema)

    if tableName.endswith("clusterData"):
        table.range_partitioning = bigquery.RangePartitioning(
            field="starttime",
            range_=bigquery.PartitionRange(start=1514745000000, end=1893436200000, interval=86400000)
        )
    elif tableName.endswith("unifiedTable") or tableName.endswith("preAggregated"):
        table.time_partitioning = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="startTime"  # name of column to use for partitioning
        )

    elif tableName.endswith("awsEc2Inventory") or tableName.endswith("awsEc2InventoryTemp") or tableName.endswith("awsEbsInventory") or tableName.endswith("awsEbsInventoryTemp"):
        table.time_partitioning = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="lastUpdatedAt"
        )
    elif tableName.endswith("awsEc2InventoryCPU") or tableName.endswith("awsEbsInventoryMetrics"):
        table.time_partitioning = bigquery.TimePartitioning(
            type_=bigquery.TimePartitioningType.DAY,
            field="addedAt"
        )


    try:
        table = client.create_table(table)  # Make an API request.
        print_("Created table {}.{}.{}".format(table.project, table.dataset_id, table.table_id))
    except Exception as e:
        print_("Error while creating table\n {}".format(e), "WARN")

