import boto3
import base64
import datetime
import json
import io
import os
import util

from botocore.config import Config
from google.cloud import bigquery
from google.cloud import secretmanager
from util import create_dataset, if_tbl_exists, createTable, print_

"""
{
	"accountId": "vzybqdfrslesqo3cmb90ag",
	"accountIdOrig": "vZYBQdFRSlesqo3CMB90Ag",
	"datasetName": "BillingReport_vzybqdfrslesqo3cmb90ag",
	"roleArn": "arn:aws:iam::448640225317:role/harnessContinuousEfficiencyRole",
	"externalId": "harness:891928451355:wFHXHD0RRQWoO8tIZT5YVw",
	"linkedAccountId": "448640225317"
}

TODO: Finalize this
	"ceProjectId": "",
	"ceOrgId": "",
"""

EC2_DATA_MAP = []
TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"

# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html
STATIC_REGION = ['af-south-1',
                 'ap-east-1',
                 'ap-northeast-1',
                 'ap-northeast-2',
                 'ap-northeast-3',
                 'ap-south-1',
                 'ap-southeast-1',
                 'ap-southeast-2',
                 'ca-central-1',
                 'eu-central-1',
                 'eu-north-1',
                 'eu-south-1',
                 'eu-west-1',
                 'eu-west-2',
                 'eu-west-3',
                 'me-south-1',
                 'sa-east-1',
                 'us-east-1',
                 'us-east-2',
                 'us-west-1',
                 'us-west-2']

# TODO: Move this to util
def get_secret_key(jsonData, key):
    client = secretmanager.SecretManagerServiceClient()
    secret_name = key
    project_id = jsonData["projectName"]
    request = {"name": f"projects/{project_id}/secrets/{secret_name}/versions/latest"}
    response = client.access_secret_version(request)
    secret_string = response.payload.data.decode("UTF-8")
    return secret_string

# TODO: Move this to util
def assumed_role_session(jsonData):
    """
    :return: Access key, Secret Key and Session Token
    """
    roleArn, roleSessionName, externalId = jsonData["roleArn"], jsonData["accountIdOrig"], jsonData["externalId"]
    sts_client = boto3.client('sts', aws_access_key_id=get_secret_key(jsonData, "CE_AWS_ACCESS_KEY_GCPSM"),
                              aws_secret_access_key=get_secret_key(jsonData, "CE_AWS_SECRET_ACCESS_KEY_GCPSM"))
    assumed_role_object = sts_client.assume_role(
        RoleArn=roleArn,
        RoleSessionName=roleSessionName,
        ExternalId=externalId,
    )
    credentials = assumed_role_object['Credentials']
    return credentials['AccessKeyId'], credentials['SecretAccessKey'], credentials['SessionToken']


def get_ec2_data(jsonData):
    my_config = Config(
        region_name='us-west-2',  # initial region to call aws api first time
        retries={
            'max_attempts': 2,
            'mode': 'standard'
        }
    )

    try:
        print_("Assuming role")
        key, secret, token = assumed_role_session(jsonData)
    except Exception as e:
        print_(e, "ERROR")
        raise e

    ec2 = boto3.client('ec2', config=my_config, aws_access_key_id=key,
                       aws_secret_access_key=secret, aws_session_token=token)
    try:
        response = ec2.describe_regions()
        REGIONS = [item["RegionName"] for item in response['Regions']]
    except Exception as e:
        # We probably do not have describe region permission in CF template for this customer.
        print_(e, "ERROR")
        print_("Using static region list")
        REGIONS = STATIC_REGION

    for region in REGIONS:
        ec2 = boto3.client('ec2', region_name=region, aws_access_key_id=key,
                           aws_secret_access_key=secret, aws_session_token=token)
        print_("Getting all instances for region - %s" % region)
        try:
            allinstances = ec2.describe_instances()
        except Exception as e:
            print_(e, "ERROR")
            print_("Error calling describe region for %s" % region)
            continue
        instance_count = 0
        for instances_ in allinstances["Reservations"]:
            owner_account = instances_["OwnerId"]
            for instance in instances_["Instances"]:
                try:
                    launchTime = instance["LaunchTime"]
                    if isinstance(instance["LaunchTime"], datetime.datetime):
                        launchTime = instance["LaunchTime"].__str__()
                    EC2_DATA_MAP.append({
                        "linkedAccountId": owner_account,
                        "instanceId": instance["InstanceId"],
                        "instanceType": instance["InstanceType"],
                        "region": region,
                        "availabilityZone": instance["Placement"]["AvailabilityZone"],
                        "tenancy": instance["Placement"]["Tenancy"],
                        "publicIpAddress": instance.get("PublicIpAddress"),
                        "state": instance["State"]["Name"],
                        "labels": instance.get("Tags"),
                        "cpuAvg": "",  # will be updated by CF2
                        "cpuMin": "",
                        "cpuMax": "",
                        "lastUpdatedAt": datetime.datetime.utcnow().__str__(),
                        "instanceLaunchedAt": launchTime
                    })
                    instance_count += 1
                except Exception as e:
                    print_("Error in instance data :\n %s" % instance, 'ERROR')
                    print_(e)
        print_("Found %s instances in region %s" % (instance_count, region))
    return EC2_DATA_MAP


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
    # This is available only in runtime python 3.7, go 1.11
    jsonData["projectName"] = os.environ.get('GCP_PROJECT', 'ccm-play')

    client = bigquery.Client(jsonData["projectName"])

    # Set the accountId for GCP logging
    util.ACCOUNTID_LOG = jsonData.get("accountIdOrig")

    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    awsEc2InventoryTableRef = dataset.table("awsEc2Inventory")
    awsEc2InventoryTableRefTemp = dataset.table("awsEc2InventoryTemp")
    awsEc2InventoryTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "awsEc2Inventory")
    awsEc2InventoryTableNameTemp = TABLE_NAME_FORMAT % (
        jsonData["projectName"], jsonData["accountId"], "awsEc2InventoryTemp")

    if not if_tbl_exists(client, awsEc2InventoryTableRef):
        print_("%s table does not exists, creating table..." % awsEc2InventoryTableRef)
        createTable(client, awsEc2InventoryTableName)
    if not if_tbl_exists(client, awsEc2InventoryTableRefTemp):
        print_("%s table does not exists, creating table..." % awsEc2InventoryTableRefTemp)
        createTable(client, awsEc2InventoryTableNameTemp)

    ec2_data_map = get_ec2_data(jsonData)
    print_(ec2_data_map)

    job_config = bigquery.LoadJobConfig(
        write_disposition=bigquery.WriteDisposition.WRITE_TRUNCATE,
        source_format=bigquery.SourceFormat.NEWLINE_DELIMITED_JSON,
    )
    b = u'%s' % ('\n'.join([json.dumps(record) for record in ec2_data_map]))
    data_as_file = io.StringIO(b)
    job = client.load_table_from_file(data_as_file, awsEc2InventoryTableNameTemp, job_config=job_config)
    print_(job.job_id)
    job.result()

    query = """
    MERGE %s T
    USING %s S
    ON T.InstanceId = S.InstanceId
    WHEN MATCHED THEN
      UPDATE SET publicIpAddress = s.publicIpAddress, state = s.state, lastUpdatedAt = '%s'
    WHEN NOT MATCHED THEN
      INSERT (linkedAccountId, instanceId, instanceType, 
        region, availabilityZone, tenancy, publicIpAddress, state, labels, cpuAvg, cpuMin, 
        cpuMax, lastUpdatedAt, instanceLaunchedAt) 
      VALUES(linkedAccountId, instanceId, instanceType, 
        region, availabilityZone, tenancy, publicIpAddress, state, labels, cpuAvg, cpuMin, 
        cpuMax, lastUpdatedAt, instanceLaunchedAt) 
    """ % (awsEc2InventoryTableName, awsEc2InventoryTableNameTemp, datetime.datetime.utcnow())
    print_(query)

    query_job = client.query(query)
    query_job.result()  # wait for job to complete
    print_("Completed")
