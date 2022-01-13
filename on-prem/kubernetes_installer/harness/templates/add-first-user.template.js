/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

use harness;

function addFirstUser() {

    if (0 < db.accounts.count()) {
        print("Data found in DB. No need of seeding the initial accounts data again.");
        return;
    }

    print("No data found in DB. Seeding initial accounts data into it.");

    db.getCollection('accounts').insert({
        "_id" : "{{ .Values.accounts.accountId }}",
        "companyName" : "{{ .Values.accounts.companyName }}",
        "accountName" : "{{ .Values.accounts.accountName }}",
        "accountKey" : "{{ .Values.accounts.accountSecret }}",
        "licenseExpiryTime" : NumberLong(-1),
        "appId" : "__GLOBAL_APP_ID__",
        "createdAt" : NumberLong(1518718220245),
        "lastUpdatedAt" : NumberLong(1518718221042)
    });


    db.getCollection('roles').insert({
        "_id" : "-3CVPDZYRyGVM3Bs7yQoAg",
        "name" : "Account Administrator",
        "accountId" : "{{ .Values.accounts.accountId }}",
        "roleType" : "ACCOUNT_ADMIN",
        "allApps" : false,
        "appId" : "__GLOBAL_APP_ID__",
        "createdAt" : NumberLong(1518718220312),
        "lastUpdatedAt" : NumberLong(1518718221044)
    });


    db.getCollection('roles').insert({
        "_id" : "cSk3N98XQde9N9wqV6Q2Aw",
        "name" : "Application Administrator",
        "accountId" : "{{ .Values.accounts.accountId }}",
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
        "accountId" : "{{ .Values.accounts.accountId }}",
        "isDefault" : true,
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
                "USER_PERMISSION_READ",
                "MANAGE_APPLICATIONS",
                "TEMPLATE_MANAGEMENT"
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
        "accountId" : "{{ .Values.accounts.accountId }}",
        "isDefault" : true,
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
        "accountId" : "{{ .Values.accounts.accountId }}",
        "isDefault" : true,
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
        "email" : "{{ .Values.accounts.adminEmail }}",
        "passwordHash" : "$2a$10$Rf/.q4HvUkS7uG2Utdkk7.jLnqnkck5ruH/vMrHjGVk4R9mL8nQE2",
        "roles" : [
            "-3CVPDZYRyGVM3Bs7yQoAg"
        ],
        "accounts" : [
            "{{ .Values.accounts.accountId }}"
        ],
        "lastLogin" : NumberLong(0),
        "emailVerified" : true,
        "statsFetchedOn" : NumberLong(0),
        "passwordChangedAt" : NumberLong(1518718220556),
        "appId" : "__GLOBAL_APP_ID__",
        "createdAt" : NumberLong(1518718220557),
        "lastUpdatedAt" : NumberLong(1518718221043)
    });

    db.getCollection('loginSettings').insert({
         "_id" : "8hZ0sj0zQc69235lOQoTHA",
         "className" : "software.wings.beans.loginSettings.LoginSettings",
         "accountId" : '{{ .Values.accounts.accountId }}',
         "lastUpdatedAt" : NumberLong(1561665380309),
         "userLockoutPolicy" : {
             "enableLockoutPolicy" : false,
             "numberOfFailedAttemptsBeforeLockout" : NumberInt(5),
             "lockOutPeriod" : NumberInt(24),
             "notifyUser" : true
         },
         "passwordExpirationPolicy" : {
             "enabled" : false,
             "daysBeforePasswordExpires" : NumberInt(90),
             "daysBeforeUserNotifiedOfPasswordExpiration" : NumberInt(5)
         },
         "passwordStrengthPolicy" : {
             "enabled" : false,
             "minNumberOfCharacters" : NumberInt(0),
             "minNumberOfUppercaseCharacters" : NumberInt(0),
             "minNumberOfLowercaseCharacters" : NumberInt(0),
             "minNumberOfSpecialCharacters" : NumberInt(0),
             "minNumberOfDigits" : NumberInt(0)
         }
     });
}

addFirstUser();
