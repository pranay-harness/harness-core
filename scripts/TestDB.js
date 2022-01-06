/*
 * Copyright 2016 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

// start mongo client and run following command -> 'load("scripts/TestDB.js");'

var DB = connect('127.0.0.1:27017/wings');

var devENV = {
    "_id" : "852E4A1399664BABBA8C47BEEB985BC8",
    "name" : "DEV",
    "description" : "This is a sample environment",
    "createdAt" : NumberLong(1460084089758),
    "lastUpdatedAt" : NumberLong(1460084089758),
    "active" : true
}

var prodENV = {
    "_id" : "91B4106B92FC4711BC020362ABD5778A",
    "name" : "PROD",
    "description" : "This is a sample environment",
    "createdAt" : NumberLong(1460095377749),
    "lastUpdatedAt" : NumberLong(1460095377748),
    "active" : true
}

var uatENV = {
    "_id" : "7485F3446272488A9B55C83D78587B3C",
    "name" : "UAT",
    "description" : "This is a sample environment",
    "createdAt" : NumberLong(1460095390406),
    "lastUpdatedAt" : NumberLong(1460095390406),
    "active" : true
}

var qaENV = {
    "_id" : "61A365AB2950496E813F1EF21B5E99F6",
    "name" : "QA",
    "description" : "This is a sample environment",
    "createdAt" : NumberLong(1460095397463),
    "lastUpdatedAt" : NumberLong(1460095397463),
    "active" : true
}

DB.environments.insert([devENV, prodENV, uatENV, qaENV]);


var accountService = {
    "_id" : "3D23ED45CB2E4522B24D78B40EE8C31A",
    "name" : "Account Service",
    "description" : "This is a sample service",
    "createdAt" : NumberLong(1460083660162),
    "lastUpdatedAt" : NumberLong(1460083660162),
    "active" : true
}

var catalogService = {
    "_id" : "041EEA73ED594DF2AA33869DF6D71159",
    "name" : "Catalog Service",
    "description" : "This is a sample service",
    "createdAt" : NumberLong(1460096455519),
    "lastUpdatedAt" : NumberLong(1460096455518),
    "active" : true
}

var orderService = {
    "_id" : "11B60E0111DB4F128262B8A2F3CED51D",
    "name" : "Order Service",
    "description" : "This is a sample service",
    "createdAt" : NumberLong(1460096468958),
    "lastUpdatedAt" : NumberLong(1460096468958),
    "active" : true
}

var fulfillmentService = {
    "_id" : "0C5515EC984840FCA336D1174BF09134",
    "name" : "Fulfillment Service",
    "description" : "This is a sample service",
    "createdAt" : NumberLong(1460096500246),
    "lastUpdatedAt" : NumberLong(1460096500246),
    "active" : true
}

var oemService = {
    "_id" : "D7363BD5A6294F01BE9D7F449443198F",
    "name" : "OEM Service",
    "description" : "This is a sample service",
    "createdAt" : NumberLong(1460096512298),
    "lastUpdatedAt" : NumberLong(1460096512298),
    "active" : true
}

DB.services.insert([accountService, catalogService, orderService, fulfillmentService, oemService]);

var appA = {
    "_id" : "C4FE0DF38E5E4A1AA8D8456A2E7953E6",
    "name" : "eCommerce AppA",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083138806),
    "lastUpdatedAt" : NumberLong(1460083138804),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

var appB = {
    "_id" : "BDD916D9B34144D3A8D1740D9E7BD618",
    "name" : "eCommerce AppB",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083138806),
    "lastUpdatedAt" : NumberLong(1460083138804),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

var appC = {
    "_id" : "38AEBBCABA4D4237B99B428C15F973D1",
    "name" : "eCommerce AppC",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083138806),
    "lastUpdatedAt" : NumberLong(1460083138804),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

var appD = {
    "_id" : "A8B8D8345FC04F8CA81D224C304B4764",
    "name" : "eCommerce AppD",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083181261),
    "lastUpdatedAt" : NumberLong(1460083181261),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

var appE = {
    "_id" : "C42B879C50724F5DB7813F084C7C626D",
    "name" : "eCommerce AppE",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083186201),
    "lastUpdatedAt" : NumberLong(1460083186201),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

var appF = {
    "_id" : "35D856C672CE4D9291D50F68255E0186",
    "name" : "eCommerce AppF",
    "description" : "This is a sample application",
    "createdAt" : NumberLong(1460083190627),
    "lastUpdatedAt" : NumberLong(1460083190626),
    "active" : true,
    "services" : [
        "3D23ED45CB2E4522B24D78B40EE8C31A",
        "041EEA73ED594DF2AA33869DF6D71159",
        "11B60E0111DB4F128262B8A2F3CED51D",
        "0C5515EC984840FCA336D1174BF09134",
        "D7363BD5A6294F01BE9D7F449443198F"
    ],
    "environments" : [
        "852E4A1399664BABBA8C47BEEB985BC8",
        "91B4106B92FC4711BC020362ABD5778A",
        "7485F3446272488A9B55C83D78587B3C",
        "61A365AB2950496E813F1EF21B5E99F6"
    ]
}

DB.applications.insert([appA, appB, appC, appD, appE, appF]);
