from google.cloud import bigquery
from google.cloud.exceptions import NotFound
from clusterdata_schema import clusterDataTableFields
from unified_schema import unifiedTableTableSchema
from preaggregated_schema import preAggreagtedTableSchema

def create_dataset(client, jsonData):
    dataset_id = "{}.{}".format(client.project, jsonData["datasetName"])
    dataset = bigquery.Dataset(dataset_id)
    dataset.location = "US"

    # Send the dataset to the API for creation, with an explicit timeout.
    # Raises google.api_core.exceptions.Conflict if the Dataset already
    # exists within the project.
    try:
        dataset = client.create_dataset(dataset, timeout=30)  # Make an API request.
        print("Created dataset {}.{}".format(client.project, dataset.dataset_id))
    except Exception as e:
        print("Dataset {} already exists {}".format(dataset_id, e))


def if_tbl_exists(client, table_ref):
    try:
        client.get_table(table_ref)
        return True
    except NotFound:
        return False


def createTable(client, tableName):
    print("Creating %s table" % tableName)
    schema = []
    if tableName.endswith("clusterData"):
        fieldset = clusterDataTableFields
    elif tableName.endswith("preAggregated"):
        fieldset = preAggreagtedTableSchema
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
    try:
        table = client.create_table(table)  # Make an API request.
        print("Created table {}.{}.{}".format(table.project, table.dataset_id, table.table_id))
    except Exception as e:
        print("Error while creating table\n {}".format(e))

