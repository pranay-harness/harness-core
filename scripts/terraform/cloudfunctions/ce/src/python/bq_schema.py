"""
Holds all schemas

Generated by command:
bq show --format=prettyjson ce-prod-274307:BillingReport_wfhxhd0rrqwoo8tizt5yvw.preAggregated | jq
"""

aws_cur_table_schema = [
    {
        "mode": "NULLABLE",
        "name": "resourceid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "usagestartdate",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "productname",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "productfamily",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "servicecode",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "blendedrate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "blendedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "unblendedrate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "unblendedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "availabilityzone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "usageaccountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "instancetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "usagetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "lineitemtype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "effectivecost",
        "type": "FLOAT"
    },
    {
        "fields": [
            {
                "mode": "NULLABLE",
                "name": "key",
                "type": "STRING"
            },
            {
                "mode": "NULLABLE",
                "name": "value",
                "type": "STRING"
            }
        ],
        "mode": "REPEATED",
        "name": "tags",
        "type": "RECORD"
    }
]


preAggreagtedTableSchema = [
    {
        "mode": "NULLABLE",
        "name": "cost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpProduct",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpSkuId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpSkuDescription",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "startTime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpProjectId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "zone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpBillingAccountId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudProvider",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsBlendedRate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsBlendedCost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUnblendedRate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUnblendedCost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "awsServicecode",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsAvailabilityzone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUsageaccountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsInstancetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUsagetype",
        "type": "STRING"
    },
    {
        "name": "discount",
        "type": "FLOAT"
    },
    {
        "name": "azureServiceName",
        "type": "STRING"
    },
    {
        "name": "azureResourceRate",
        "type": "FLOAT"
    },
    {
        "name": "azureSubscriptionGuid",
        "type": "STRING"
    },
    {
        "name": "azureTenantId",
        "type": "STRING"
    }
]

clusterDataTableFields = [
    {
        "mode": "REQUIRED",
        "name": "starttime",
        "type": "INTEGER"
    },
    {
        "mode": "REQUIRED",
        "name": "endtime",
        "type": "INTEGER"
    },
    {
        "mode": "REQUIRED",
        "name": "accountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "settingid",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "instanceid",
        "type": "STRING"
    },
    {
        "mode": "REQUIRED",
        "name": "instancetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "billingaccountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clusterid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clustername",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "appid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "serviceid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "envid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "appname",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "servicename",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "envname",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudproviderid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "parentinstanceid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "launchtype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clustertype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "workloadname",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "workloadtype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "namespace",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudservicename",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "taskid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudprovider",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "billingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpubillingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memorybillingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "idlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memoryidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "usagedurationseconds",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuunitseconds",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memorymbseconds",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "maxcpuutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "maxmemoryutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "avgcpuutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "avgmemoryutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "systemcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpusystemcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memorysystemcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "actualidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuactualidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memoryactualidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "unallocatedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpuunallocatedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memoryunallocatedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "instancename",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cpurequest",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memoryrequest",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpulimit",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memorylimit",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "maxcpuutilizationvalue",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "maxmemoryutilizationvalue",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "avgcpuutilizationvalue",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "avgmemoryutilizationvalue",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "networkcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "pricingsource",
        "type": "STRING"
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
        "name": "label",
        "type": "RECORD"
    },
    {
        "mode": "NULLABLE",
        "name": "storagecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "storagembseconds",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "storagerequest",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "storageutilizationvalue",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "storageunallocatedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "storageactualidlecost",
        "type": "FLOAT"
    }
]

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
    {
        "mode": "REPEATED",
        "name": "volumeIds",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "instanceLifeCycle",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "reservationId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "stateTransitionReason",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "linkedAccountIdPartition",
        "type": "INTEGER"
    },
]

unifiedTableTableSchema = [
    {
        "mode": "REQUIRED",
        "name": "startTime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "cost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpProduct",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpSkuId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpSkuDescription",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpProjectId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "region",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "zone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "gcpBillingAccountId",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudProvider",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsBlendedRate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsBlendedCost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUnblendedRate",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUnblendedCost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "awsServicecode",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsAvailabilityzone",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUsageaccountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsInstancetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "awsUsagetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "discount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "endtime",
        "type": "TIMESTAMP"
    },
    {
        "mode": "NULLABLE",
        "name": "accountid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "instancetype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clusterid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clustername",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "appid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "serviceid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "envid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudproviderid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "launchtype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clustertype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "workloadname",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "workloadtype",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "namespace",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "cloudservicename",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "taskid",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "clustercloudprovider",
        "type": "STRING"
    },
    {
        "mode": "NULLABLE",
        "name": "billingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "cpubillingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "memorybillingamount",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "idlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "maxcpuutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "avgcpuutilization",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "systemcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "actualidlecost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "unallocatedcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "networkcost",
        "type": "FLOAT"
    },
    {
        "mode": "NULLABLE",
        "name": "product",
        "type": "STRING"
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
    {
        "name": "azureMeterCategory",
        "type": "STRING"
    },
    {
        "name": "azureMeterSubcategory",
        "type": "STRING"
    },
    {
        "name": "azureMeterId",
        "type": "STRING"
    },
    {
        "name": "azureMeterName",
        "type": "STRING"
    },
    {
        "name": "azureResourceType",
        "type": "STRING"
    },
    {
        "name": "azureServiceTier",
        "type": "STRING"
    },
    {
        "name": "azureInstanceId",
        "type": "STRING"
    },
    {
        "name": "azureResourceGroup",
        "type": "STRING"
    },
    {
        "name": "azureSubscriptionGuid",
        "type": "STRING"
    },
    {
        "name": "azureAccountName",
        "type": "STRING"
    },
    {
        "name": "azureFrequency",
        "type": "STRING"
    },
    {
        "name": "azurePublisherType",
        "type": "STRING"
    },
    {
        "name": "azurePublisherName",
        "type": "STRING"
    },
    {
        "name": "azureServiceName",
        "type": "STRING"
    },
    {
        "name": "azureSubscriptionName",
        "type": "STRING"
    },
    {
        "name": "azureReservationId",
        "type": "STRING"
    },
    {
        "name": "azureReservationName",
        "type": "STRING"
    },
    {
        "name": "azureResource",
        "type": "STRING"
    },
    {
        "name": "azureVMProviderId",
        "type": "STRING"
    },
    {
        "name": "azureTenantId",
        "type": "STRING"
    },
    {
        "name": "azureBillingCurrency",
        "type": "STRING"
    },
    {
        "name": "azureCustomerName",
        "type": "STRING"
    },
]


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
        "mode": "NULLABLE",
        "name": "kmsKeyId",
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
        "name": "linkedAccountIdPartition",
        "type": "INTEGER"
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