
awsEc2InventoryCPUSchema = [
        {
            "mode": "NULLABLE",
            "name": "instanceId",
            "type": "STRING"
        },
        {
            "mode": "NULLABLE",
            "name": "average",
            "type": "FLOAT"
        },
        {
            "mode": "NULLABLE",
            "name": "minimum",
            "type": "FLOAT"
        },
        {
            "mode": "NULLABLE",
            "name": "maximum",
            "type": "FLOAT"
        },
        {
            "mode": "NULLABLE",
            "name": "addedAt",
            "type": "TIMESTAMP"
        },
        {
            "mode": "NULLABLE",
            "name": "metricStartTime",
            "type": "TIMESTAMP"
        },
        {
            "mode": "NULLABLE",
            "name": "metricEndTime",
            "type": "TIMESTAMP"
        },
    ]

awsEc2InventorySchema = [
    {
        "mode": "NULLABLE",
        "name": "tenancy",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "state",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "publicIpAddress",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "linkedAccountId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "instanceType",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "instanceId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "availabilityZone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuAvg",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuMin",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuMax",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "lastUpdatedAt",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "instanceLaunchedAt",
        "type": "TIMESTAMP"
    },
    {
        "fields": [
            {
                "name": "key",
                "type": "STRING"
            },
            {
                "name": "value",
                "type": "STRING"
            }
        ],
        "mode": "REPEATED",
        "name": "labels",
        "type": "RECORD"
    },
]