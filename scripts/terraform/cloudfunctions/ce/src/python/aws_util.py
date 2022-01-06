# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from google.cloud import secretmanager
import boto3

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


def get_secret_key(jsonData, key):
    client = secretmanager.SecretManagerServiceClient()
    secret_name = key
    project_id = jsonData["projectName"]
    request = {"name": f"projects/{project_id}/secrets/{secret_name}/versions/latest"}
    response = client.access_secret_version(request)
    secret_string = response.payload.data.decode("UTF-8")
    return secret_string


def assumed_role_session(jsonData):
    """
    :return: Access key, Secret Key and Session Token
    """
    roleArn, roleSessionName, externalId = jsonData["roleArn"], jsonData["accountId"], jsonData["externalId"]
    sts_client = boto3.client('sts',
                              aws_access_key_id=get_secret_key(jsonData, "CE_AWS_ACCESS_KEY_GCPSM"),
                              aws_secret_access_key=get_secret_key(jsonData, "CE_AWS_SECRET_ACCESS_KEY_GCPSM"))
    assumed_role_object = sts_client.assume_role(
        RoleArn=roleArn,
        RoleSessionName=roleSessionName,
        ExternalId=externalId,
    )
    credentials = assumed_role_object['Credentials']
    return credentials['AccessKeyId'], credentials['SecretAccessKey'], credentials['SessionToken']
