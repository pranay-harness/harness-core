# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import json
import os

from google.auth import impersonated_credentials
from google.cloud import secretmanager
from google.oauth2 import service_account

PROJECTID = os.environ.get('GCP_PROJECT', 'ccm-play')
KEY = "CCM_GCP_CREDENTIALS"

STATIC_ZONES_MAPPING = {'asia-east1-a': 'asia-east1',
                        'asia-east1-b': 'asia-east1',
                        'asia-east1-c': 'asia-east1',
                        'asia-east2-c': 'asia-east2',
                        'asia-east2-b': 'asia-east2',
                        'asia-east2-a': 'asia-east2',
                        'asia-northeast1-a': 'asia-northeast1',
                        'asia-northeast1-b': 'asia-northeast1',
                        'asia-northeast1-c': 'asia-northeast1',
                        'asia-northeast2-b': 'asia-northeast2',
                        'asia-northeast2-c': 'asia-northeast2',
                        'asia-northeast2-a': 'asia-northeast2',
                        'asia-northeast3-a': 'asia-northeast3',
                        'asia-northeast3-c': 'asia-northeast3',
                        'asia-northeast3-b': 'asia-northeast3',
                        'asia-south1-b': 'asia-south1',
                        'asia-south1-a': 'asia-south1',
                        'asia-south1-c': 'asia-south1',
                        'asia-southeast1-a': 'asia-southeast1',
                        'asia-southeast1-b': 'asia-southeast1',
                        'asia-southeast1-c': 'asia-southeast1',
                        'asia-southeast2-a': 'asia-southeast2',
                        'asia-southeast2-c': 'asia-southeast2',
                        'asia-southeast2-b': 'asia-southeast2',
                        'australia-southeast1-c': 'australia-southeast1',
                        'australia-southeast1-a': 'australia-southeast1',
                        'australia-southeast1-b': 'australia-southeast1',
                        'europe-central2-b': 'europe-central2',
                        'europe-central2-c': 'europe-central2',
                        'europe-central2-a': 'europe-central2',
                        'europe-north1-b': 'europe-north1',
                        'europe-north1-c': 'europe-north1',
                        'europe-north1-a': 'europe-north1',
                        'europe-west1-b': 'europe-west1',
                        'europe-west1-c': 'europe-west1',
                        'europe-west1-d': 'europe-west1',
                        'europe-west2-a': 'europe-west2',
                        'europe-west2-b': 'europe-west2',
                        'europe-west2-c': 'europe-west2',
                        'europe-west3-c': 'europe-west3',
                        'europe-west3-a': 'europe-west3',
                        'europe-west3-b': 'europe-west3',
                        'europe-west4-c': 'europe-west4',
                        'europe-west4-b': 'europe-west4',
                        'europe-west4-a': 'europe-west4',
                        'europe-west6-b': 'europe-west6',
                        'europe-west6-c': 'europe-west6',
                        'europe-west6-a': 'europe-west6',
                        'northamerica-northeast1-a': 'northamerica-northeast1',
                        'northamerica-northeast1-b': 'northamerica-northeast1',
                        'northamerica-northeast1-c': 'northamerica-northeast1',
                        'southamerica-east1-a': 'southamerica-east1',
                        'southamerica-east1-b': 'southamerica-east1',
                        'southamerica-east1-c': 'southamerica-east1',
                        'us-central1-a': 'us-central1',
                        'us-central1-b': 'us-central1',
                        'us-central1-c': 'us-central1',
                        'us-central1-f': 'us-central1',
                        'us-east1-b': 'us-east1',
                        'us-east1-c': 'us-east1',
                        'us-east1-d': 'us-east1',
                        'us-east4-a': 'us-east4',
                        'us-east4-b': 'us-east4',
                        'us-east4-c': 'us-east4',
                        'us-west1-a': 'us-west1',
                        'us-west1-b': 'us-west1',
                        'us-west1-c': 'us-west1',
                        'us-west2-c': 'us-west2',
                        'us-west2-b': 'us-west2',
                        'us-west2-a': 'us-west2',
                        'us-west3-a': 'us-west3',
                        'us-west3-b': 'us-west3',
                        'us-west3-c': 'us-west3',
                        'us-west4-c': 'us-west4',
                        'us-west4-a': 'us-west4',
                        'us-west4-b': 'us-west4'}

def get_impersonated_credentials(jsonData):
    # Get source credentials
    target_scopes = [
        'https://www.googleapis.com/auth/cloud-platform']
    json_acct_info = json.loads(get_secret_key())
    credentials = service_account.Credentials.from_service_account_info(json_acct_info)
    source_credentials = credentials.with_scopes(target_scopes)

    # Impersonate to target credentials
    target_credentials = impersonated_credentials.Credentials(
        source_credentials=source_credentials,
        target_principal=jsonData["serviceAccount"],
        target_scopes=target_scopes,
        lifetime=500)
    print("source: %s, target: %s" % (target_credentials._source_credentials.service_account_email,
                                      target_credentials.service_account_email))
    return target_credentials


def get_secret_key():
    client = secretmanager.SecretManagerServiceClient()
    request = {"name": f"projects/{PROJECTID}/secrets/{KEY}/versions/latest"}
    response = client.access_secret_version(request)
    secret_string = response.payload.data.decode("UTF-8")
    return secret_string
