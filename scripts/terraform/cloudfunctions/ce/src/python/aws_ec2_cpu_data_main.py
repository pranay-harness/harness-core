import boto3
import base64
import json
import io
import os
import util

from pprint import pprint
from datetime import datetime, date, time, timedelta
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

TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"


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
    :param roleArn:
    :param roleSessionName:
    :param externalId:
    :return: Access key, Secret Key and Session Token
    """
    roleArn, roleSessionName, externalId = jsonData["roleArn"], jsonData["accountIdOrig"], jsonData["externalId"]
    sts_client = boto3.client('sts', aws_access_key_id=get_secret_key(jsonData, "CE_AWS_ACCESS_KEY_GCPSM"),
                              aws_secret_access_key=get_secret_key(jsonData, "CE_AWS_SECRET_ACCESS_KEY_GCPSM"))
    assumed_role_object = sts_client.assume_role(
        RoleArn=roleArn,  # "arn:aws:iam::448640225317:role/harnessContinuousEfficiencyRole",
        RoleSessionName=roleSessionName,  # "wFHXHD0RRQWoO8tIZT5YVw",
        ExternalId=externalId,  # "harness:891928451355:wFHXHD0RRQWoO8tIZT5YVw",
    )
    credentials = assumed_role_object['Credentials']
    return credentials['AccessKeyId'], credentials['SecretAccessKey'], credentials['SessionToken']


def get_ec2_regions(jsonData):
    """
    Returns unique regions from BQ table
    :param jsonData:
    :return:
    """
    ec2_regions_instance_map = {}
    client = bigquery.Client(jsonData["projectName"])
    awsEc2InventoryTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "awsEc2Inventory")
    query = """
            SELECT distinct(region), InstanceId FROM %s where state="running";
		    """ % (awsEc2InventoryTableName)

    query_job = client.query(query)
    results = query_job.result()  # wait for job to complete
    for row in results:
        try:
            ec2_regions_instance_map[row.region].append(row.InstanceId)
        except KeyError:
            ec2_regions_instance_map[row.region] = [row.InstanceId]
    print_(ec2_regions_instance_map)
    return ec2_regions_instance_map


def get_ec2_cpu_data(jsonData):
    """
    We need to compute CPU for currently running EC2s in a region.
    This function iterates through unique regions in BQ for running EC2s.
    Calls the aws api in batches.

    :param jsonData:
    :return ec2_data_map:
    {'i-i0fd782575c43d5f08': {'Average': 0.14150364748452063,
                          'Maximum': 14.4262295081955,
                          'Minimum': 0.0}}
    """
    ec2_data_map = {}
    ec2_regions_instance_map = get_ec2_regions(jsonData)
    key, secret, token = assumed_role_session(jsonData)
    print_("Assumed role")
    added_at = datetime.utcnow().__str__()
    print_("Getting CPU data")
    for region in ec2_regions_instance_map:
        cloudwatch = boto3.client('cloudwatch', region_name=region, aws_access_key_id=key,
                                  aws_secret_access_key=secret, aws_session_token=token)
        MetricDataQueries = []
        metric_start_time = datetime.combine(date.today() - timedelta(days=1), time())
        metric_end_time = datetime.combine(date.today(), time())
        print_("Fetching CPU data for instances in region: %s " % region)
        for instanceId in ec2_regions_instance_map[region]:
            # get_metric_data supports 500 queries at once.
            if len(MetricDataQueries) + 3 <= 500:
                # Each EC2 instance we need to get average, minimum and maximum CPU for past 1 day
                MetricDataQueries.extend([{
                    'Id': 'maximum_%s' % instanceId.replace("i-", ""),
                    'MetricStat': {
                        'Metric': {
                            'Namespace': "AWS/EC2",
                            'MetricName': "CPUUtilization",
                            'Dimensions': [
                                {
                                    "Name": "InstanceId",
                                    "Value": instanceId
                                },
                            ]
                        },
                        'Period': 86400,
                        'Stat': 'Maximum'
                    }
                },
                    {
                        'Id': 'minimum_%s' % instanceId.replace("i-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EC2",
                                'MetricName': "CPUUtilization",
                                'Dimensions': [
                                    {
                                        "Name": "InstanceId",
                                        "Value": instanceId
                                    },
                                ]
                            },
                            'Period': 86400,
                            'Stat': 'Minimum'
                        }
                    },
                    {
                        'Id': 'average_%s' % instanceId.replace("i-", ""),
                        'MetricStat': {
                            'Metric': {
                                'Namespace': "AWS/EC2",
                                'MetricName': "CPUUtilization",
                                'Dimensions': [
                                    {
                                        "Name": "InstanceId",
                                        "Value": instanceId
                                    },
                                ]
                            },
                            'Period': 86400,
                            'Stat': 'Average'
                        }
                    }])
            else:
                # We have close to 500 MetricDataQueries. Fire the api call
                # TODO: This else and below else can be converted to a function
                try:
                    print_("Size of MetricDataQueries: %s. firing get_metric_data" % len(MetricDataQueries))
                    response = cloudwatch.get_metric_data(
                        MetricDataQueries=MetricDataQueries,
                        StartTime=metric_start_time,
                        EndTime=metric_end_time
                    )
                    # reset to 0
                    MetricDataQueries = []
                    MetricDataResults = response.get('MetricDataResults')
                    for metric in MetricDataResults:
                        instanceId = "i-%s" % metric["Id"].split("_")[1]
                        stat = metric["Id"].split("_")[0]
                        try:
                            ec2_data_map[instanceId][stat] = metric["Values"][0]
                        except IndexError:
                            # Probably this instance has been just stopped.
                            print_("Stat %s not found for instance %s" % (stat, instanceId), "WARN")
                            continue
                        except KeyError:
                            ec2_data_map[instanceId] = {stat: metric["Values"][0],
                                                        "instanceId": instanceId,
                                                        "addedAt": added_at,
                                                        "metricStartTime": metric_start_time.__str__(),
                                                        "metricEndTime": metric_end_time.__str__()}
                except Exception as e:
                    print_(e)
                    raise e
        else:
            if not len(MetricDataQueries) > 0:
                continue
            print_("Size of MetricDataQueries: %s. firing get_metric_data" % len(MetricDataQueries))
            # We have leftover entries in MetricDataQueries. Fire the api call
            try:
                response = cloudwatch.get_metric_data(
                    MetricDataQueries=MetricDataQueries,
                    StartTime=metric_start_time,
                    EndTime=metric_end_time
                )
                # reset to 0
                MetricDataResults = response.get('MetricDataResults')
                for metric in MetricDataResults:
                    instanceId = "i-%s" % metric["Id"].split("_")[1]
                    stat = metric["Id"].split("_")[0]
                    try:
                        ec2_data_map[instanceId][stat] = metric["Values"][0]
                    except IndexError:
                        # Probably this instance has been just stopped.
                        print_("Stat %s not found for instance %s" % (stat, instanceId), "WARN")
                        continue
                    except KeyError:
                        ec2_data_map[instanceId] = {stat: metric["Values"][0],
                                                    "instanceId": instanceId,
                                                    "addedAt": added_at,
                                                    "metricStartTime": metric_start_time.__str__(),
                                                    "metricEndTime": metric_end_time.__str__()}
            except Exception as e:
                print_(e)
                raise e
    if len(ec2_regions_instance_map) == 0:
        print_("No instances found to fetch CPU data for")
    return ec2_data_map, added_at


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
    jsonData["projectName"] = os.environ.get('GCP_PROJECT', 'ccm-play')

    client = bigquery.Client(jsonData["projectName"])

    # Set the accountId for GCP logging
    util.ACCOUNTID_LOG = jsonData.get("accountIdOrig")

    create_dataset(client, jsonData["datasetName"])
    dataset = client.dataset(jsonData["datasetName"])

    awsEc2InventoryCPUTableRef = dataset.table("awsEc2InventoryCPU")
    awsEc2InventoryCPUTableName = TABLE_NAME_FORMAT % (
        jsonData["projectName"], jsonData["accountId"], "awsEc2InventoryCPU")
    awsEc2InventoryTableName = TABLE_NAME_FORMAT % (jsonData["projectName"], jsonData["accountId"], "awsEc2Inventory")

    if not if_tbl_exists(client, awsEc2InventoryCPUTableRef):
        print_("%s table does not exists, creating table..." % awsEc2InventoryCPUTableRef)
        createTable(client, awsEc2InventoryCPUTableName)

    ec2_data_map, added_at = get_ec2_cpu_data(jsonData)
    print_("Total instances for which CPU data was fetched: %s" % len(ec2_data_map))
    if len(ec2_data_map) == 0:
        print_("Nothing to update")
        return
    job_config = bigquery.LoadJobConfig(
        write_disposition=bigquery.WriteDisposition.WRITE_APPEND,
        source_format=bigquery.SourceFormat.NEWLINE_DELIMITED_JSON,
    )
    b = u'%s' % ('\n'.join([json.dumps(ec2_data_map[record]) for record in ec2_data_map]))
    data_as_file = io.StringIO(b)
    job = client.load_table_from_file(data_as_file, awsEc2InventoryCPUTableName, job_config=job_config)
    print_(job.job_id)
    job.result()

    query = """
    MERGE %s T
    USING %s S
    ON T.instanceId = S.instanceId AND S.addedAt = '%s'
    WHEN MATCHED THEN
      UPDATE SET cpuAvg = s.average, cpuMin = s.minimum, cpuMax = s.maximum, lastUpdatedAt = '%s'
    """ % (awsEc2InventoryTableName, awsEc2InventoryCPUTableName, added_at, datetime.utcnow())
    print_(query)
    query_job = client.query(query)
    query_job.result()  # wait for job to complete
    print_("Completed")


"""
get_metric_data response:

{'Messages': [],
 'MetricDataResults': [{'Id': 'i0fd782575c43d5f08_Maximum',
                        'Label': 'i-0fd782575c43d5f08 Maximum',
                        'StatusCode': 'Complete',
                        'Timestamps': [datetime.datetime(2021, 5, 2, 0, 0, tzinfo=tzutc())],
                        'Values': [14.4262295081955]},
                       {'Id': 'i0fd782575c43d5f08_Average',
                        'Label': 'i-0fd782575c43d5f08 Average',
                        'StatusCode': 'Complete',
                        'Timestamps': [datetime.datetime(2021, 5, 2, 0, 0, tzinfo=tzutc())],
                        'Values': [0.14150364748452066]},
                       ....],
                       
 'ResponseMetadata': {'HTTPHeaders': {'content-length': '1530',
                                      'content-type': 'text/xml',
                                      'date': 'Mon, 03 May 2021 10:16:12 GMT',
                                      'x-amzn-requestid': 'a9314d72-ebcc-44f5-9973-3326aeb4bfa3'},
                      'HTTPStatusCode': 200,
                      'RequestId': 'a9314d72-ebcc-44f5-9973-3326aeb4bfa3',
                      'RetryAttempts': 0}}
"""
