import io
import json
import base64
import os
import re
from google.cloud import bigquery
from google.cloud.exceptions import NotFound
from google.oauth2 import service_account
from googleapiclient import discovery
from google.cloud import recommender

PARENT_PARAM = "projects/%s/locations/%s/recommenders/%s"
PARENT_PARAM_FOR_INSIGHTS = "projects/%s/locations/%s/insightTypes/%s"

RESOURCE_URL = "https://console.cloud.google.com/compute/%ssDetail/zones/%s/%ss/%s?project=%s"

RECOMMENDATION_TYPES = ['google.compute.disk.IdleResourceRecommender',
                        'google.compute.address.IdleResourceRecommender',
                        'google.compute.image.IdleResourceRecommender',
                        'google.compute.instance.IdleResourceRecommender']

INSIGHT_TYPES = ['google.compute.disk.IdleResourceInsight',
                 'google.compute.image.IdleResourceInsight',
                 'google.compute.address.IdleResourceInsight']

NANO = 1000000000

TABLE_NAME_FORMAT = "%s.BillingReport_%s.%s"
recommendationsTableSchema = [
    {
        "mode": "REQUIRED",
        "name": "resource",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "recommendationSubtype",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "location",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "resourceType",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "description",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "insight",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "resourceLink",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "currency",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "costPeriod",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "projectId",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "lastUpdatedAt",
        "type": "TIMESTAMP"
    }
]

STATIC_LOCATIONS_MAPPING = {'asia-east1-a': 'asia-east1', 'asia-east1-b': 'asia-east1', 'asia-east1-c': 'asia-east1', 'asia-east2-c': 'asia-east2', 'asia-east2-b': 'asia-east2', 'asia-east2-a': 'asia-east2', 'asia-northeast1-a': 'asia-northeast1', 'asia-northeast1-b': 'asia-northeast1', 'asia-northeast1-c': 'asia-northeast1', 'asia-northeast2-b': 'asia-northeast2', 'asia-northeast2-c': 'asia-northeast2', 'asia-northeast2-a': 'asia-northeast2', 'asia-northeast3-a': 'asia-northeast3', 'asia-northeast3-c': 'asia-northeast3', 'asia-northeast3-b': 'asia-northeast3', 'asia-south1-b': 'asia-south1', 'asia-south1-a': 'asia-south1', 'asia-south1-c': 'asia-south1', 'asia-southeast1-a': 'asia-southeast1', 'asia-southeast1-b': 'asia-southeast1', 'asia-southeast1-c': 'asia-southeast1', 'asia-southeast2-a': 'asia-southeast2', 'asia-southeast2-c': 'asia-southeast2', 'asia-southeast2-b': 'asia-southeast2', 'australia-southeast1-c': 'australia-southeast1', 'australia-southeast1-a': 'australia-southeast1', 'australia-southeast1-b': 'australia-southeast1', 'europe-central2-b': 'europe-central2', 'europe-central2-c': 'europe-central2', 'europe-central2-a': 'europe-central2', 'europe-north1-b': 'europe-north1', 'europe-north1-c': 'europe-north1', 'europe-north1-a': 'europe-north1', 'europe-west1-b': 'europe-west1', 'europe-west1-c': 'europe-west1', 'europe-west1-d': 'europe-west1', 'europe-west2-a': 'europe-west2', 'europe-west2-b': 'europe-west2', 'europe-west2-c': 'europe-west2', 'europe-west3-c': 'europe-west3', 'europe-west3-a': 'europe-west3', 'europe-west3-b': 'europe-west3', 'europe-west4-c': 'europe-west4', 'europe-west4-b': 'europe-west4', 'europe-west4-a': 'europe-west4', 'europe-west6-b': 'europe-west6', 'europe-west6-c': 'europe-west6', 'europe-west6-a': 'europe-west6', 'northamerica-northeast1-a': 'northamerica-northeast1', 'northamerica-northeast1-b': 'northamerica-northeast1', 'northamerica-northeast1-c': 'northamerica-northeast1', 'southamerica-east1-a': 'southamerica-east1', 'southamerica-east1-b': 'southamerica-east1', 'southamerica-east1-c': 'southamerica-east1', 'us-central1-a': 'us-central1', 'us-central1-b': 'us-central1', 'us-central1-c': 'us-central1', 'us-central1-f': 'us-central1', 'us-east1-b': 'us-east1', 'us-east1-c': 'us-east1', 'us-east1-d': 'us-east1', 'us-east4-a': 'us-east4', 'us-east4-b': 'us-east4', 'us-east4-c': 'us-east4', 'us-west1-a': 'us-west1', 'us-west1-b': 'us-west1', 'us-west1-c': 'us-west1', 'us-west2-c': 'us-west2', 'us-west2-b': 'us-west2', 'us-west2-a': 'us-west2', 'us-west3-a': 'us-west3', 'us-west3-b': 'us-west3', 'us-west3-c': 'us-west3', 'us-west4-c': 'us-west4', 'us-west4-a': 'us-west4', 'us-west4-b': 'us-west4'}

def getBqClient():
    credentials = service_account.Credentials.from_service_account_file('credentials.json')
    project_id = 'ccm-play'
    client = bigquery.Client(credentials= credentials,project=project_id)
    return client

def getRecommendations(projectId, projectNumber):
    client = recommender.RecommenderClient()
    locations, locationToRegionMapping = getLocations(projectId)
    # locations = ['us-central1-c']
    recommendations = []
    metadata = dict()
    validLocations = []
    for recommendationType in RECOMMENDATION_TYPES:
        resourceType = recommendationType.split('.')[2].upper()
        print(resourceType)
        for location in locations:
            parent = PARENT_PARAM % (projectNumber, location, recommendationType)
            recommendationsIterator = client.list_recommendations(parent=parent).__iter__()
            recommnedationCount = 0
            for recommendation in recommendationsIterator:
                recommendationMetadata = dict()
                recommendations.append(recommendation)
                recommendationMetadata['location'] = location
                recommendationMetadata['region'] = locationToRegionMapping[location]
                recommendationMetadata['resourceType'] = resourceType
                recommendationMetadata['projectId'] = projectId
                recommendationMetadata['projectNumber'] = projectNumber
                metadata[recommendation.name] = recommendationMetadata
                recommnedationCount += 1
                # print(recommendation)

            if recommnedationCount != 0:
                validLocations.append(location)
    return recommendations, metadata, validLocations

def getInsights(projectId, projectNumber, locations):
    client = recommender.RecommenderClient()
    insights = dict()
    for insightType in INSIGHT_TYPES:
        for location in locations:
            parent = PARENT_PARAM_FOR_INSIGHTS % (projectNumber, location, insightType)
            insightsIterator = client.list_insights(parent=parent).__iter__()
            for insight in insightsIterator:
                insights[insight.name] = insight
                # print(insight)
    return insights

def getLocations(projectId):
    locations = []
    locationToRegionMapping = dict()
    try:
        service = discovery.build('compute', 'v1')
        request = service.regions().list(project=projectId)
        while request is not None:
            response = request.execute()
            for region in response['items']:
                regionName = region['name']
                zonesList = region['zones']
                for zone in zonesList:
                    zoneName = zone.split('/')[-1]
                    locations.append(zoneName)
                    locationToRegionMapping[zoneName] = regionName
            request = service.regions().list_next(previous_request=request, previous_response=response)
    except Exception as e:
        locationToRegionMapping = STATIC_LOCATIONS_MAPPING
        for key in locationToRegionMapping:
            locations.append(key)
        print("Exception in getting locations for project: " + str(e))

    return locations, locationToRegionMapping

def getDataToInsert(recommendations, recommendationsMetadata, insights):
    data = []
    for recommendation in recommendations:
        recommendationData = getDataFromRecommendation(recommendation, recommendationsMetadata, insights)
        data.append({
            "resource": recommendationData.get('resource'),
            "recommendationSubtype": recommendationData.get('recommenderSubtype'),
            "location": recommendationData.get('location'),
            "region": recommendationData.get('region'),
            "resourceType": recommendationData.get('resourceType'),
            "description": recommendationData.get('description'),
            "insight": recommendationData.get('insight'),
            "resourceLink": recommendationData.get('resourceLink'),
            "cost": recommendationData.get('cost'),
            "currency": recommendationData.get('currency'),
            "costPeriod": recommendationData.get('costPeriod'),
            "projectId": recommendationData.get('projectId'),
            "lastUpdatedAt": recommendationData.get('lastUpdatedAt')
        })
    return data

def getDataFromRecommendation(recommendation, recommendationsMetadata, insights):
    data = dict()

    # To get cost data
    try:
        data['cost'] = recommendation.primary_impact.cost_projection.cost.nanos/NANO + recommendation.primary_impact.cost_projection.cost.units
        data['currency'] = recommendation.primary_impact.cost_projection.cost.currency_code
        data['costPeriod'] = str(recommendation.primary_impact.cost_projection.duration)
    except Exception as e:
        print("Exception in getting cost data from recommendation:" + str(e))

    # To get general description
    try:
        data['description'] = recommendation.description
        data['resource'] = recommendation.description.split(' ')[-1].split('\'')[1]
        data['recommenderSubtype'] = recommendation.recommender_subtype
        data['lastUpdatedAt'] = str(recommendation.last_refresh_time)
    except Exception as e:
        print("Exception in getting data from recommendation: " + str(e))

    # To get insights data
    try:
        insight = recommendation.associated_insights[0].insight
        data['insight'] = insights.get(insight).description
    except Exception as e:
        print("Exception in getting insight data from recommendation:" + str(e))

    # Recommendations metadata
    try:
        name = recommendation.name
        metadata = recommendationsMetadata[name]
        data['location'] = metadata['location']
        data['region'] = metadata['region']
        data['resourceType'] = metadata['resourceType']
        data['projectId'] = metadata['projectId']
        data['resourceLink'] = RESOURCE_URL % (metadata['resourceType'].lower(), metadata['location'], metadata['resourceType'].lower(), data['resource'], metadata['projectNumber'])
    except Exception as e:
        print("Exception in getting recommendation metadata:" + str(e))

    return data

def insertDataInTable(client, rows):
    job_config = bigquery.LoadJobConfig(
        write_disposition=bigquery.WriteDisposition.WRITE_TRUNCATE,
        source_format=bigquery.SourceFormat.NEWLINE_DELIMITED_JSON,
    )

    rowsJson = u'%s' % ('\n'.join([json.dumps(row) for row in rows]))
    data_as_file = io.StringIO(rowsJson)

    # Set tableId here in this format "your-project.your_dataset.your_table"
    _tableId = "ccm-play.BillingReport_kmpysmuisimorrjl6nl73w.recommendations"
    client.load_table_from_file(data_as_file, _tableId, job_config=job_config)

# Methods for table creation
def create_dataset(client, datasetName, accountId):
    dataset_id = "{}.{}".format(client.project, datasetName)
    dataset = bigquery.Dataset(dataset_id)
    dataset.location = "US"
    dataset.description = "Dataset for [ AccountId: %s ]" % (accountId)

    # Send the dataset to the API for creation, with an explicit timeout.
    # Raises google.api_core.exceptions.Conflict if the Dataset already
    # exists within the project.
    try:
        dataset = client.create_dataset(dataset, timeout=30)  # Make an API request.
    except Exception as e:
        print("Dataset already exists" + str(e))


def if_tbl_exists(client, table_ref):
    try:
        client.get_table(table_ref)
        return True
    except NotFound:
        return False


def createTable(client, tableName):
    print("Creating %s table" % tableName)
    schema = []
    fieldset = recommendationsTableSchema

    for field in fieldset:
        if field.get("type") == "RECORD":
            nested_field = [bigquery.SchemaField(nested_field["name"], nested_field["type"], mode=nested_field.get("mode", "")) for nested_field in field["fields"]]
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field["mode"], fields=nested_field))
        else:
            schema.append(bigquery.SchemaField(field["name"], field["type"], mode=field.get("mode", "")))
    table = bigquery.Table(tableName, schema=schema)

    try:
        table = client.create_table(table)  # Make an API request.
        print("Created table for recommendations")
    except Exception as e:
        print("Error while creating recommendations table" + str(e))

"""
{
	"accountId": "vZYBQdFRSlesqo3CMB90Ag",
	"projectId": "ccm-play",
	"projectNumber": "project-number-here"
}
"""

def main(event, context):

    data = base64.b64decode(event['data']).decode('utf-8')
    jsonData = json.loads(data)
    print(jsonData)

    projectId = os.environ.get('GCP_PROJECT', 'ccm-play')
    accountId = jsonData.get("accountId")
    client = bigquery.Client(projectId)
    accountIdBQ = re.sub('[^0-9a-z]', '_', accountId.lower())
    datasetName = "BillingReport_%s" % accountIdBQ
    create_dataset(client, datasetName, accountId)

    dataset = client.dataset(datasetName)

    # Setting table names for main and temp tables
    tableRef = dataset.table("recommendations")
    tableName = TABLE_NAME_FORMAT % (projectId, accountIdBQ, "recommendations")

    # Creating tables if they don't exist
    if not if_tbl_exists(client, tableRef):
        print("%s table does not exists, creating table..." % tableName)
        createTable(client, tableName)

    recommendations, recommendationsMetadata, locations = getRecommendations(jsonData.get("projectId"), jsonData.get("projectNumber"))
    insights = getInsights(jsonData.get("projectId"), jsonData.get("projectNumber"), locations)
    data = getDataToInsert(recommendations, recommendationsMetadata, insights)
    insertDataInTable(client, data)
    print("Done")



