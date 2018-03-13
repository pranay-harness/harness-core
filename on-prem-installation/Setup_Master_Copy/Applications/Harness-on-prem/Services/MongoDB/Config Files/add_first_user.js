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
