awsEbsInventorySchema = [
    {
        "mode": "REQUIRED",
        "name": "lastUpdatedAt",
        "type": "TIMESTAMP"
    },
    {
        "mode": "REQUIRED",
        "name": "volumeId",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "createTime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "availabilityZone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "encrypted",
        "type": "BOOLEAN"
    },
    {
        "mode": "NULLABLE",
        "name": "size",
        "type": "INTEGER"
    },
    {
        "mode": "NULLABLE",
        "name": "state",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "iops",
        "type": "INTEGER"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeType",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "multiAttachedEnabled",
        "type": "BOOLEAN"
    },
    {
        "mode": "NULLABLE",
        "name": "detachedAt",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "deleteTime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "snapshotId",
        "type": "STRING"
    },
    {
        "fields": [
            {
                "name": "attachTime",
                "type": "TIMESTAMP"
            },
            {
                "name": "device",
                "type": "STRING"
            },
            {
                "name": "instanceId",
                "type": "STRING"
            },
            {
                "name": "state",
                "type": "STRING"
            },
            {
                "name": "volumeId",
                "type": "STRING"
            },
            {
                "name": "deleteOnTermination",
                "type": "BOOLEAN"
            }
        ],
        "mode": "REPEATED",
        "name": "attachments",
        "type": "RECORD"
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
        "name": "tags",
        "type": "RECORD"
    },
    {
        "mode": "NULLABLE",
        "name": "linkedAccountId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeReadBytes",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeWriteBytes",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeReadOps",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeWriteOps",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeIdleTime",
        "type": "FLOAT"
    }
]

awsEbsInventoryMetricsSchema = [
    {
        "mode": "REQUIRED",
        "name": "volumeId",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "addedAt",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeReadBytes",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeWriteBytes",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeReadOps",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeWriteOps",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "volumeIdleTime",
        "type": "FLOAT"
    },
    {
        "mode": "REQUIRED",
        "name": "metricStartTime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "REQUIRED",
        "name": "metricEndTime",
        "type": "TIMESTAMP"
    }
]