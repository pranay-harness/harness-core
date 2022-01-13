# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import boto3
import datetime
import json
import io
import os
import re
import base64
from google.cloud import bigquery
from util import create_dataset, if_tbl_exists, createTable, print_, ACCOUNTID_LOG, TABLE_NAME_FORMAT
from aws_util import assumed_role_session, get_secret_key


def get_regions_and_volumes(jsonData):
    REGIONS_VOLUME_MAP = {}
    client = bigquery.Client(jsonData["projectName"])
    awsEc2InventoryTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountIdBQ"], "awsEbsInventory")
    query = """
            SELECT distinct(region), volumeId FROM %s where state="in-use";
		    """ % (awsEc2InventoryTableName)

    query_job = client.query(query)
    results = query_job.result()  # wait for job to complete
    for row in results:
        try:
            REGIONS_VOLUME_MAP[row.region].append(row.volumeId)
        except KeyError:
            REGIONS_VOLUME_MAP[row.region] = [row.volumeId]
    print_(REGIONS_VOLUME_MAP)
    return REGIONS_VOLUME_MAP


def get_ebs_metrics_data(jsonData):
    EBS_DATA_MAP = {}
    REGIONS_VOLUME_MAP = get_regions_and_volumes(jsonData)
    key, secret, token = assumed_role_session(jsonData)
    added_at = datetime.datetime.utcnow()
    for region in REGIONS_VOLUME_MAP:
        queries_formed = 0
        MetricDataQueries = []
        cloudwatch = boto3.client('cloudwatch', region_name=region, aws_access_key_id=key,
                                  aws_secret_access_key=secret, aws_session_token=token)
        for volumeId in REGIONS_VOLUME_MAP[region]:
            # get_metric_data supports 500 queries at once.
            if queries_formed + 5 <= 500:
                # For each ebs volume, we are getting Sum(VolumeReadBytes), Sum(VolumeWriteBytes), Sum(VolumeReadOps), Sum(VolumeWriteOps), Sum(VolumeIdleTime)
                MetricDataQueries.extend([{
                    'Id': 'vol_%s_volumeReadBytes' % volumeId.replace("vol-", ""),
                    'MetricStat': {
                        'Metric': {
                            'Namespace': "AWS/EBS",
                            'MetricName': "VolumeReadBytes",
                            'Dimensions': [
                                {
                                    'Name': "VolumeId",
                                    'Value': volumeId
                                }
                            ]
                        },
                        'Period': 86400,
                        'Stat': "Sum",
                        'Unit': "Bytes"
                    }
                },
                    {
                        'Id': 'vol_%s_volumeWriteBytes' % volumeId.replace("vol-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EBS",
                                'MetricName': "VolumeWriteBytes",
                                'Dimensions': [
                                    {
                                        'Name': "VolumeId",
                                        'Value': volumeId
                                    }
                                ]
                            },
                            'Period': 86400,
                            'Stat': "Sum",
                            'Unit': "Bytes"
                        }
                    },
                    {
                        'Id': 'vol_%s_volumeReadOps' % volumeId.replace("vol-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EBS",
                                'MetricName': "VolumeReadOps",
                                'Dimensions': [
                                    {
                                        'Name': "VolumeId",
                                        'Value': volumeId
                                    }
                                ]
                            },
                            'Period': 86400,
                            'Stat': "Sum",
                            'Unit': "Count"
                        }
                    },
                    {
                        'Id': 'vol_%s_volumeWriteOps' % volumeId.replace("vol-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EBS",
                                'MetricName': "VolumeWriteOps",
                                'Dimensions': [
                                    {
                                        'Name': "VolumeId",
                                        'Value': volumeId
                                    }
                                ]
                            },
                            'Period': 86400,
                            'Stat': "Sum",
                            'Unit': "Count"
                        }
                    },
                    {
                        'Id': 'vol_%s_volumeIdleTime' % volumeId.replace("vol-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EBS",
                                'MetricName': "VolumeIdleTime",
                                'Dimensions': [
                                    {
                                        'Name': "VolumeId",
                                        'Value': volumeId
                                    }
                                ]
                            },
                            'Period': 86400,
                            'Stat': "Sum",
                            'Unit': "Seconds"
                        }
                    }])
                queries_formed += 5
            else:
                EBS_DATA_MAP = executeQueries(MetricDataQueries, cloudwatch, added_at, EBS_DATA_MAP)
                queries_formed = 0
                MetricDataQueries = []

        if queries_formed > 0:
            EBS_DATA_MAP = executeQueries(MetricDataQueries, cloudwatch, added_at, EBS_DATA_MAP)

    return EBS_DATA_MAP, added_at


def executeQueries(MetricDataQueries, cloudwatch, added_at, EBS_DATA_MAP):
    # We have close to 500 MetricDataQueries. Fire the api call
    startTime = datetime.datetime.combine(datetime.date.today() - datetime.timedelta(days=1), datetime.time())
    endTime = datetime.datetime.combine(datetime.date.today(), datetime.time())
    try:
        response = cloudwatch.get_metric_data(
            MetricDataQueries=MetricDataQueries,
            StartTime=startTime,
            EndTime=endTime
        )

        MetricDataResults = response.get('MetricDataResults')
        for metric in MetricDataResults:
            volumeId = "vol-%s" % metric["Id"].split("_")[1]
            stat = metric["Id"].split("_")[2]
            try:
                EBS_DATA_MAP[volumeId][stat] = metric["Values"][0]
            except IndexError:
                # Probably this instance has been just stopped.
                continue
            except KeyError:
                EBS_DATA_MAP[volumeId] = {stat: metric["Values"][0],
                                          "volumeId": volumeId,
                                          "addedAt": str(added_at),
                                          "metricStartTime": str(startTime),
                                          "metricEndTime": str(endTime)}

        return EBS_DATA_MAP
    except Exception as e:
        print_(e)
        raise e


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

    jsonData["accountIdBQ"] = re.sub('[^0-9a-z]', '_', jsonData.get("accountId").lower())
    jsonData["datasetName"] = "BillingReport_%s" % jsonData["accountIdBQ"]
    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    awsEbsInventoryMetricsTableRef = dataset.table("awsEbsInventoryMetrics")
    awsEbsInventoryMetricsTableName = TABLE_NAME_FORMAT % (
        jsonData["projectName"], jsonData["accountIdBQ"], "awsEbsInventoryMetrics")

    if not if_tbl_exists(client, awsEbsInventoryMetricsTableRef):
        print_("%s table does not exists, creating table..." % awsEbsInventoryMetricsTableRef)
        createTable(client, awsEbsInventoryMetricsTableRef)

    data_map, added_at = get_ebs_metrics_data(jsonData)
    print_("Total volumes for which Metrics data was fetched: %s" % len(data_map))
    if len(data_map) == 0:
        print_("Nothing to update")
        return
    job_config = bigquery.LoadJobConfig(
        write_disposition=bigquery.WriteDisposition.WRITE_APPEND,
        source_format=bigquery.SourceFormat.NEWLINE_DELIMITED_JSON,
    )
    b = u'%s' % ('\n'.join([json.dumps(data_map[record]) for record in data_map]))
    data_as_file = io.StringIO(b)
    job = client.load_table_from_file(data_as_file, awsEbsInventoryMetricsTableName, job_config=job_config)
    print_(job.job_id)
    job.result()
    print_("Completed")
