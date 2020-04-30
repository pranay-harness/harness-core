/**
 * This is a set of shortcuts to mongo queries for use in Robo 3T.
 * Store it in your home directory as .mongorc.js and select "Load .mongorc.js"
 * from the Options menu in Robo, then restart.
 *
 * Type Harness. and select a function. Most take no arguments.
 * Ones like NDays take a number of days.
 *
 * Harness.toCSV(<some-query>) outputs all result rows in CSV so you can copy into a spreadsheet.
 *
 * You can nest these like:
 *
 * Harness.toCSV(Harness.connectedDelegateMachines())
 */
var Harness = function() {

    return {

        // Produce CSV output from any cursor
        toCSV: function(cursor) {

            var count = -1;
            var headers = [];
            var data = {};

            deliminator = ',';
            textQualifier = '\"';

            while (cursor.hasNext()) {

                var array = new Array(cursor.next());

                count++;

                for (var index in array[0]) {
                    if (headers.indexOf(index) == -1) {
                        headers.push(index);
                    }
                }

                for (var i = 0; i < array.length; i++) {
                    for (var index in array[i]) {
                        data[count + '_' + index] = array[i][index];
                    }
                }
            }

            var line = '';

            for (var index in headers) {
                line += textQualifier + headers[index] + textQualifier + deliminator;
            }

            line = line.slice(0, -1);
            print(line);

            for (var i = 0; i < count + 1; i++) {

                var line = '';
                var cell = '';
                for (var j = 0; j < headers.length; j++) {
                    cell = data[i + '_' + headers[j]];
                    if (cell == undefined) cell = '';
                    line += textQualifier + cell + textQualifier + deliminator;
                }

                line = line.slice(0, -1);
                print(line);
            }
        },

        // Count the connected delegate processes per account
        connectedDelegateProcesses: function() {

            return db.delegateConnections.aggregate([
                {$group:{
                    _id:"$accountId",
                    connections: { $sum: NumberInt(1) }
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    accountName: "$account_docs.accountName",
                    connections: 1
                }},
                {$sort:{connections:-1, accountName:1}}
            ]);

        },

        // Count the connected delegate IDs per account
        connectedDelegateMachines: function() {

            return db.delegateConnections.aggregate([
                {$group:{
                    _id:"$accountId",
                    distinctDelegateIds: { $addToSet: "$delegateId" }
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    accountName: "$account_docs.accountName",
                    connectedDelegates: {$size: "$distinctDelegateIds"}
                }},
                {$sort:{connectedDelegates:-1, accountName:1}}
            ]);

        },

        // Count connected delegates versus to total number of delegate records per account
        connectedDelegatesVsTotalDelegates: function() {

            return db.delegates.aggregate([
                {$lookup:{
                    from:"delegateConnections",
                    localField:"_id",
                    foreignField:"delegateId",
                    as:"connection_docs"
                }},
                {$unwind: {path:"$connection_docs", preserveNullAndEmptyArrays: true}},
                {$group:{
                    _id:"$accountId",
                    delegates: {$sum: NumberInt(1)},
                    distinctConnections: { $addToSet: "$connection_docs.delegateId" }
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    accountName: "$account_docs.accountName",
                    delegates: 1,
                    connectedDelegates: {$size: "$distinctConnections"}
                }},
                {$sort:{connectedDelegates:-1, accountName:1}}
            ]);

        },

        // Count connected delegates per account with a list of versions those delegates have
        connectedDelegatesByAccountWithVersion: function() {

            return db.delegateConnections.aggregate([
                {$group:{
                    _id:"$accountId",
                    distinctDelegateIds: { $addToSet: "$delegateId" },
                    distinctVersions: { $addToSet: "$version" }
                }},
                // Sort versions
                {$unwind: "$distinctVersions"},
                {$sort: {distinctVersions: 1}},
                {$group:{
                    _id:"$_id",
                    distinctDelegateIds: {$first:"$distinctDelegateIds"},
                    distinctVersions: {$push:"$distinctVersions"}
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    accountName: "$account_docs.accountName",
                    connectedDelegates: {$size: "$distinctDelegateIds"},
                    distinctVersions:{$reduce:{
                        input: "$distinctVersions",
                        initialValue: "",
                        in: {$concat: [
                            "$$value",
                            // Separator between values
                            {$cond: [
                                {$gt:[{"$strLenCP": "$$value"}, 0]},
                                "  |  ",
                                ""
                            ]},
                            "$$this"
                        ]}
                    }}
                }},
                {$sort:{connectedDelegates:-1, accountName:1}}
            ]);

        },

        // List the delegate hostnames and account along with versions each host is running
        connectedDelegateHostNamesAndVersions: function() {

            return db.delegates.aggregate([
                {$lookup:{
                    from:"delegateConnections",
                    localField:"_id",
                    foreignField:"delegateId",
                    as:"connection_docs"
                }},
                {$unwind: {path:"$connection_docs", preserveNullAndEmptyArrays: true}},
                {$group:{
                    _id:"$_id",
                    accountId: {$first:"$accountId"},
                    hostName: {$first:"$hostName"},
                    distinctVersions: {$addToSet:"$connection_docs.version"}
                }},
                // Sort versions
                {$unwind: "$distinctVersions"},
                {$sort: {distinctVersions: 1}},
                {$group:{
                    _id:"$_id",
                    accountId: {$first:"$accountId"},
                    hostName: {$first:"$hostName"},
                    distinctVersions: {$push:"$distinctVersions"}
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"accountId",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    _id: 0,
                    accountName: "$account_docs.accountName",
                    hostName: 1,
                    distinctVersions:{$reduce:{
                        input: "$distinctVersions",
                        initialValue: "",
                        in: {$concat: [
                            "$$value",
                            // Separator between values
                            {$cond: [
                                {$gt:[{"$strLenCP": "$$value"}, 0]},
                                "  |  ",
                                ""
                            ]},
                            "$$this"
                        ]}
                    }}
                }},
                {$sort:{accountName:1, hostName:1}}
            ]);

        },

        // List the connected delegates that have exactly one version running
        connectedDelegatesWithOneVersion: function() {

            return db.delegates.aggregate([
                {$lookup:{
                    from:"delegateConnections",
                    localField:"_id",
                    foreignField:"delegateId",
                    as:"connection_docs"
                }},
                {$unwind: {path:"$connection_docs", preserveNullAndEmptyArrays: true}},
                {$group:{
                    _id:"$_id",
                    accountId: {$first:"$accountId"},
                    hostName: {$first:"$hostName"},
                    distinctVersions: {$addToSet:"$connection_docs.version"}
                }},
                {$match:{distinctVersions: {"$size": 1}}},
                {$lookup:{
                    from:"accounts",
                    localField:"accountId",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: {path:"$account_docs", preserveNullAndEmptyArrays: true}},
                {$project:{
                    _id: 0,
                    accountName: "$account_docs.accountName",
                    hostName: 1,
                    version:{ $arrayElemAt: [ "$distinctVersions", 0 ] }
                }},
                {$sort:{accountName:1, hostName:1}}
            ]);

        },

        // Count the running executions per account
        runningExecutionsAccount: function() {

            return db.workflowExecutions.aggregate([
                {$match:{
                    status:"RUNNING",
                    workflowType: {$ne : "PIPELINE"}
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"accountId",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: "$account_docs"},
                {$group:{
                    _id:"$account_docs._id",
                    accountName:{$first:"$account_docs.accountName"},
                    running:{$sum:NumberInt(1)}
                }},
                {$sort:{running:-1, accountName:1}}
            ]);

        },

        // Count running executions per application
        runningExecutionsApplication: function() {

            return db.workflowExecutions.aggregate([
                {$match:{
                    status:"RUNNING",
                    workflowType: {$ne : "PIPELINE"}
                }},
                {$lookup:{
                    from:"applications",
                    localField:"appId",
                    foreignField:"_id",
                    as:"app_docs"
                }},
                {$unwind: "$app_docs"},
                {$lookup:{
                    from:"accounts",
                    localField:"app_docs.accountId",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: "$account_docs"},
                {$group:{
                    _id:"$appId",
                    accountName:{$first:"$account_docs.accountName"},
                    appName:{$first:"$app_docs.name"},
                    running:{$sum:NumberInt(1)}
                }},
                {$sort:{running:-1, accountName:1, appName:1}}
            ]);

        },

        // Count the executions per account with statuses for the last N days
        executionsLastNDays: function(days) {

            return db.workflowExecutions.aggregate([
                {$match:{
                    workflowType: {$ne : "PIPELINE"},
                    status: {$ne: "RUNNING"}
                }},
                {$project:{
                    status: 1,
                    createdAt: 1,
                    accountId: 1
                }},
                {$match:{
                    createdAt: {$gte: new Date().getTime() - days * 24 * 60 * 60 * 1000}
                }},
                {$group:{
                    _id:{accountId:"$accountId", status:"$status"},
                    executed: {$sum:NumberInt(1)},
                }},
                {$group:{
                    _id:"$_id.accountId",
                    totalExecuted: {$sum:"$executed"},
                    succeeded: {$sum:{$cond:[{$eq: ["$_id.status", "SUCCESS"]}, "$executed", NumberInt(0)]}},
                    failed: {$sum:{$cond:[{$eq: ["$_id.status", "FAILED"]}, "$executed", NumberInt(0)]}},
                    aborted: {$sum:{$cond:[{$eq: ["$_id.status", "ABORTED"]}, "$executed", NumberInt(0)]}},
                    expired: {$sum:{$cond:[{$eq: ["$_id.status", "EXPIRED"]}, "$executed", NumberInt(0)]}}
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: "$account_docs"},
                {$group:{
                    _id:"$_id",
                    accountName: {$first:"$account_docs.accountName"},
                    totalExecuted: {$first:"$totalExecuted"},
                    succeeded: {$first:"$succeeded"},
                    failed: {$first:"$failed"},
                    aborted: {$first:"$aborted"},
                    expired: {$first:"$expired"}
                }},
                {$sort:{totalExecuted:-1, succeeded:-1}}
            ]);

        },

        // Count the executions for one account with statuses in the last N days
        executionsOneAccountLastNDays: function(accountId, days) {

            return db.workflowExecutions.aggregate([
                {$match:{
                    workflowType: {$ne : "PIPELINE"},
                    status: {$ne: "RUNNING"}
                }},
                {$project:{
                    status: 1,
                    createdAt: 1,
                    accountId: 1
                }},
                {$match:{
                    accountId: accountId,
                    createdAt: {$gte: new Date().getTime() - days * 24 * 60 * 60 * 1000}
                }},
                {$group:{
                    _id:{accountId:"$accountId", status:"$status"},
                    executed: {$sum:NumberInt(1)},
                }},
                {$group:{
                    _id:"$_id.accountId",
                    totalExecuted: {$sum:"$executed"},
                    succeeded: {$sum:{$cond:[{$eq: ["$_id.status", "SUCCESS"]}, "$executed", NumberInt(0)]}},
                    failed: {$sum:{$cond:[{$eq: ["$_id.status", "FAILED"]}, "$executed", NumberInt(0)]}},
                    aborted: {$sum:{$cond:[{$eq: ["$_id.status", "ABORTED"]}, "$executed", NumberInt(0)]}},
                    expired: {$sum:{$cond:[{$eq: ["$_id.status", "EXPIRED"]}, "$executed", NumberInt(0)]}}
                }},
                {$lookup:{
                    from:"accounts",
                    localField:"_id",
                    foreignField:"_id",
                    as:"account_docs"
                }},
                {$unwind: "$account_docs"},
                {$group:{
                    _id:"$_id",
                    accountName: {$first:"$account_docs.accountName"},
                    totalExecuted: {$first:"$totalExecuted"},
                    succeeded: {$first:"$succeeded"},
                    failed: {$first:"$failed"},
                    aborted: {$first:"$aborted"},
                    expired: {$first:"$expired"}
                }},
                {$sort:{totalExecuted:-1, succeeded:-1}}
            ]);

        },

        // Count the total executions per isoweek over the last N days
        executionsByWeekLastNDays: function(days) {

            return db.workflowExecutions.aggregate([
                {$match:{
                    workflowType: {$ne : "PIPELINE"},
                    status: {$ne: "RUNNING"},
                    createdAt: {$gte: new Date().getTime() - days * 24 * 60 * 60 * 1000}
                }},
                {$project:{
                    createdAt:{$add:[new Date(0), "$createdAt"]}
                }},
                {$group:{
                    _id:{year: { $isoWeekYear: "$createdAt" }, week: { $isoWeek: "$createdAt" }},
                    executed:{$sum:NumberInt(1)}
                }},
                {$project:{
                    _id:0,
                    year:"$_id.year",
                    week:"$_id.week",
                    executed:1
                }},
                {$sort:{"year":1, "week":1}}
              ]);

        }

    };

}();

