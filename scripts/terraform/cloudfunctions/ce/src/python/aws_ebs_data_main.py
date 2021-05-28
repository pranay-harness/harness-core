import io
import json
import base64
import os
from google.cloud import bigquery
from google.cloud import secretmanager
from botocore.config import Config
import datetime
import boto3
from util import create_dataset, if_tbl_exists, createTable, print_, ACCOUNTID_LOG, TABLE_NAME_FORMAT
from aws_util import assumed_role_session, STATIC_REGION, get_secret_key


def getEbsVolumesData(jsonData):
    my_config = Config(
        region_name='us-west-2',  # initial region to call aws api first time
        retries={
            'max_attempts': 2,
            'mode': 'standard'
        }
    )
    EBS_VOLUMES_DATA_MAP = []
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

        # Describe volumes call
        print_("Getting all volumes for region - %s" % region)
        try:
            allVolumes = ec2.volumes.all()
        except Exception as e:
            print_(e, "ERROR")
            print_("Error in getting volumes for %s" % region)
            continue

        # Forming rows for insertion
        volume_count = 0
        for volume in allVolumes:
            EBS_VOLUMES_DATA_MAP.append(getVolumeRow(volume, region, jsonData["linkedAccountId"]))
            volume_count += 1
        print_("Found %s instances in region %s" % (volume_count, region))

    return EBS_VOLUMES_DATA_MAP

def getVolumeRow(volumeData, region, linkedAccountId):
    return {
        "lastUpdatedAt": str(datetime.datetime.utcnow()),
        "volumeId": volumeData.volume_id,
        "createTime": str(volumeData.create_time),
        "availabilityZone": volumeData.availability_zone,
        "region": region,
        "encrypted": volumeData.encrypted,
        "size": volumeData.size,
        "state": volumeData.state,
        "iops": volumeData.iops,
        "volumeType": volumeData.volume_type,
        "multiAttachedEnabled": volumeData.multi_attach_enabled,
        "detachedAt": None,
        "deleteTime": None,
        "snapshotId": volumeData.snapshot_id,
        "attachments": getAttachments(volumeData),
        "tags": getTags(volumeData),
        "linkedAccountId": linkedAccountId,
        # Metric data will be updated by ebs metrics cf
        "volumeReadBytes": None,
        "volumeWriteBytes": None,
        "volumeReadOps": None,
        "volumeWriteOps": None,
        "volumeIdleTime": None
    }

def getAttachments(volumeData):
    attachments = []
    if volumeData.attachments is not None:
        for attachment in volumeData.attachments:
            attachments.append({
                "attachTime": str(attachment['AttachTime']),
                "device": attachment['Device'],
                "instanceId": attachment['InstanceId'],
                "state": attachment['State'],
                "volumeId": attachment['VolumeId'],
                "deleteOnTermination": attachment['DeleteOnTermination']
            })

    if len(attachments) == 0:
        return None

    return attachments

def getTags(volumeData):
    tags = []
    if volumeData.tags is not None:
        for tag in volumeData.tags:
            tags.append({
                "key": tag['Key'],
                "value": tag['Value']
            })

    if len(tags) == 0:
        return None

    return tags

# For inserting volume data in temp table
def insertDataInTempTable(client, rows, awsEbsInventoryTempTableName):
    job_config = bigquery.LoadJobConfig(
        write_disposition=bigquery.WriteDisposition.WRITE_TRUNCATE,
        source_format=bigquery.SourceFormat.NEWLINE_DELIMITED_JSON,
    )

    rowsJson = u'%s' % ('\n'.join([json.dumps(row) for row in rows]))
    data_as_file = io.StringIO(rowsJson)

    job = client.load_table_from_file(data_as_file, awsEbsInventoryTempTableName, job_config=job_config)
    print_(job.job_id)
    job.result()

# For merging temp table with main table
def mergeTempTableWithMainTable(client, awsEbsInventoryTableName, awsEbsInventoryTempTableName, currentTime):
    query = """
        MERGE %s T
        USING %s S
        ON T.volumeId = S.volumeId
        WHEN MATCHED THEN
            UPDATE SET volumeType = s.volumeType, size = s.size, state = s.state, iops = s.iops,
            attachments = s.attachments, tags = s.tags, lastUpdatedAt = '%s',
            detachedAt = 
                CASE
                    WHEN t.state = "in-use" AND s.state = "available" THEN '%s'
                    WHEN t.state = "available" AND s.state = "in-use" THEN NULL
                    ELSE t.detachedAt
                END
        WHEN NOT MATCHED THEN
            INSERT (lastUpdatedAt, volumeId, createTime, 
            availabilityZone, region, encrypted, size, state, iops, volumeType, multiAttachedEnabled, 
            detachedAt, deleteTime, snapshotId, attachments, tags, linkedAccountId, volumeReadBytes,
            volumeWriteBytes, volumeReadOps, volumeWriteOps, volumeIdleTime) 
            VALUES(lastUpdatedAt, volumeId, createTime, 
            availabilityZone, region, encrypted, size, state, iops, volumeType, multiAttachedEnabled, 
            detachedAt, deleteTime, snapshotId, attachments, tags, linkedAccountId, volumeReadBytes,
            volumeWriteBytes, volumeReadOps, volumeWriteOps, volumeIdleTime) 
    """ % (awsEbsInventoryTableName, awsEbsInventoryTempTableName, currentTime, currentTime)

    query_job = client.query(query)
    query_job.result()

# Updating rows in main table for volumes which have been deleted
def updateDeletedVolumesInMainTable(client, awsEbsInventoryTableName, currentTime):
    query = """
        UPDATE %s SET attachments = NULL, state = "deleted", lastUpdatedAt = '%s', deleteTime = '%s' WHERE lastUpdatedAt < '%s' AND state != "deleted"
    """ % (awsEbsInventoryTableName, currentTime, currentTime, currentTime)

    query_job = client.query(query)
    query_job.result()

def main(event, context):
    print(event)
    data = base64.b64decode(event['data']).decode('utf-8')
    jsonData = json.loads(data)
    print(jsonData)

    # This is available only in runtime python 3.7, go 1.11
    jsonData["projectName"] = os.environ.get('GCP_PROJECT', 'ccm-play')

    client = bigquery.Client(jsonData["projectName"])

    # Set the accountId for GCP logging
    ACCOUNTID_LOG = jsonData.get("accountIdOrig")

    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    # Setting table names for main and temp tables
    awsEbsInventoryTableRef = dataset.table("awsEbsInventory")
    awsEbsInventoryTempTableRef = dataset.table("awsEbsInventoryTemp")
    awsEbsInventoryTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "awsEbsInventory")
    awsEbsInventoryTempTableName = TABLE_NAME_FORMAT % (
        jsonData["projectName"], jsonData["accountId"], "awsEbsInventoryTemp")

    # Creating tables if they don't exist
    if not if_tbl_exists(client, awsEbsInventoryTableRef):
        print_("%s table does not exists, creating table..." % awsEbsInventoryTableRef)
        createTable(client, awsEbsInventoryTableName)
    if not if_tbl_exists(client, awsEbsInventoryTempTableRef):
        print_("%s table does not exists, creating table..." % awsEbsInventoryTempTableRef)
        createTable(client, awsEbsInventoryTempTableName)

    # Updating bq tables
    currentTime = datetime.datetime.utcnow()
    ebsVolumesDataMap = getEbsVolumesData(jsonData)
    insertDataInTempTable(client, ebsVolumesDataMap, awsEbsInventoryTempTableName)
    mergeTempTableWithMainTable(client, awsEbsInventoryTableName, awsEbsInventoryTempTableName, currentTime)
    updateDeletedVolumesInMainTable(client, awsEbsInventoryTableName, currentTime)

    print_("Completed")

