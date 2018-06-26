use HARNESSDB;
db.getCollection('accounts').insert({
    "_id" : "kmpySmUISimoRrJL6NL73w",
    "companyName" : "COMPANYNAME",
    "accountName" : "ACCOUNTNAME",
    "accountKey" : "ACCOUNT_SECRET_KEY",
    "licenseExpiryTime" : NumberLong(-1),
    "appId" : "__GLOBAL_APP_ID__",
    "createdAt" : NumberLong(1518718220245),
    "lastUpdatedAt" : NumberLong(1518718221042)
});


db.getCollection('roles').insert({
    "_id" : "-3CVPDZYRyGVM3Bs7yQoAg",
    "name" : "Account Administrator",
    "accountId" : "kmpySmUISimoRrJL6NL73w",
    "roleType" : "ACCOUNT_ADMIN",
    "allApps" : false,
    "appId" : "__GLOBAL_APP_ID__",
    "createdAt" : NumberLong(1518718220312),
    "lastUpdatedAt" : NumberLong(1518718221044)
});


db.getCollection('roles').insert({
    "_id" : "cSk3N98XQde9N9wqV6Q2Aw",
    "name" : "Application Administrator",
    "accountId" : "kmpySmUISimoRrJL6NL73w",
    "roleType" : "APPLICATION_ADMIN",
    "allApps" : true,
    "appId" : "__GLOBAL_APP_ID__",
    "createdAt" : NumberLong(1518718220321),
    "lastUpdatedAt" : NumberLong(1518718221044)
});


db.getCollection('userGroups').insert({
    "_id" : "jVT0QBGfREeaP69bV6Juiw",
    "name" : "Account Administrator",
    "description" : "Default account admin user group",
    "accountId" : "kmpySmUISimoRrJL6NL73w",
    "appPermissions" : [
        {
            "permissionType" : "ALL_APP_ENTITIES",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "actions" : [
                "UPDATE",
                "READ",
                "EXECUTE",
                "CREATE",
                "DELETE"
            ]
        }
    ],
    "accountPermissions" : {
        "permissions" : [
            "ACCOUNT_MANAGEMENT",
            "USER_PERMISSION_MANAGEMENT",
            "APPLICATION_CREATE_DELETE"
        ]
    },
    "memberIds" : [
        "c0RigPdWTlOCUeeAsdolJQ"
    ],
    "createdAt" : NumberLong(1521587621187),
    "lastUpdatedAt" : NumberLong(1521844132195)
});

db.getCollection('userGroups').insert({
    "_id" : "Piq4GXDvSDKYS5L5WRlPGA",
    "name" : "Production Support",
    "description" : "Production Support members have access to override configuration, setup infrastructure and setup/execute deployment workflows within PROD environments",
    "accountId" : "kmpySmUISimoRrJL6NL73w",
    "appPermissions" : [
        {
            "permissionType" : "ENV",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "PROD"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "SERVICE",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.GenericEntityFilter",
                "filterType" : "ALL"
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "DEPLOYMENT",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "PROD"
                ]
            },
            "actions" : [
                "READ",
                "EXECUTE"
            ]
        },
        {
            "permissionType" : "WORKFLOW",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.WorkflowFilter",
                "filterTypes" : [
                    "PROD",
                    "TEMPLATES"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "PIPELINE",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "PROD"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        }
    ],
    "memberIds" : [
        "c0RigPdWTlOCUeeAsdolJQ"
    ],
    "createdAt" : NumberLong(1521844132203),
    "lastUpdatedAt" : NumberLong(1521844132203)
});

db.getCollection('userGroups').insert({
    "_id" : "x7B0YhsJRO-Bt89pLPPUSQ",
    "name" : "Non-Production Support",
    "description" : "Non-production Support members have access to override configuration, setup infrastructure and setup/execute deployment workflows within NON_PROD environments",
    "accountId" : "kmpySmUISimoRrJL6NL73w",
    "appPermissions" : [
        {
            "permissionType" : "DEPLOYMENT",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "NON_PROD"
                ]
            },
            "actions" : [
                "READ",
                "EXECUTE"
            ]
        },
        {
            "permissionType" : "SERVICE",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.GenericEntityFilter",
                "filterType" : "ALL"
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "PIPELINE",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "NON_PROD"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "ENV",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.EnvFilter",
                "filterTypes" : [
                    "NON_PROD"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        },
        {
            "permissionType" : "WORKFLOW",
            "appFilter" : {
                "filterType" : "ALL"
            },
            "entityFilter" : {
                "className" : "software.wings.security.WorkflowFilter",
                "filterTypes" : [
                    "NON_PROD",
                    "TEMPLATES"
                ]
            },
            "actions" : [
                "READ",
                "UPDATE",
                "DELETE",
                "CREATE"
            ]
        }
    ],
    "memberIds" : [
        "c0RigPdWTlOCUeeAsdolJQ"
    ],
    "createdAt" : NumberLong(1521844132208),
    "lastUpdatedAt" : NumberLong(1521844132208)
});

db.getCollection('users').insert({
    "_id" : "c0RigPdWTlOCUeeAsdolJQ",
    "name" : "Admin",
    "email" : "EMAIL",
    "passwordHash" : "$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2",
    "roles" : [
        "-3CVPDZYRyGVM3Bs7yQoAg"
    ],
    "accounts" : [
        "kmpySmUISimoRrJL6NL73w"
    ],
    "lastLogin" : NumberLong(0),
    "emailVerified" : true,
    "statsFetchedOn" : NumberLong(0),
    "passwordChangedAt" : NumberLong(1518718220556),
    "appId" : "__GLOBAL_APP_ID__",
    "createdAt" : NumberLong(1518718220557),
    "lastUpdatedAt" : NumberLong(1518718221043)
});
