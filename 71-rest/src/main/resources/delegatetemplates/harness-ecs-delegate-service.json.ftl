{
  "serviceName": "<SERVICE_NAME>",
  "cluster": "<CLUSTER_NAME>",
  "loadBalancers": [],
  "serviceRegistries": [],
  "desiredCount": 1,
  "launchType": "EC2",
  "taskDefinition": "harness-delegate-task-spec",
  "placementConstraints": [],
  "placementStrategy": [
    {
      "type": "spread",
      "field": "attribute:ecs.availability-zone"
    },
    {
      "type": "spread",
      "field": "instanceId"
    }
  ],
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": [
        "<COMMA SEPARATED SUBNET IDS>"
      ],
      "securityGroups": [
        "<SECURITY GROUP ID>"
      ],
      "assignPublicIp": "DISABLED"
    }
  },
  "schedulingStrategy": "REPLICA"
}