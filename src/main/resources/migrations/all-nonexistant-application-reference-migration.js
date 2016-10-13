//mongeez formatted javascript
//changeset anubhawsrivastava:all-nonexistant-application-reference-migration

function DBCleanup() {
    var excludedCollections = ["system.profile", "audits", "mongeez"];
    var includedCollections = ["ALL"]; // By default goes over all collections excluding excludedCollection. Replace "ALL" with specific collections to override behavior

    if (includedCollections.length === 1 && includedCollections[0] === "ALL") {
        print("No override found for included collection. Adding all collections in includedCollections\n")
        includedCollections = db.getCollectionNames();
    }

    print("\n\nCollections to be included: " + includedCollections);
    print("\n\nCollections to be excluded: " + excludedCollections);

    var waitTime = 10;
    while (waitTime > 0) {
        sleep(1000);
        print("\nDB cleanup will start in " + waitTime + " seconds. Crl+C to cancel");
        waitTime--;
    }

    print("Preparing for cleanup");

    var applications = {"__GLOBAL_APP_ID__": []};
    var appIds = ["__GLOBAL_APP_ID__"];

    db.applications.find({}, {_id: 1}).forEach(function (app) {
        var environment = ["__GLOBAL_ENV_ID__"];
        db.environments.find({appId: app._id}, {_id: 1}).forEach(function (env) {
            environment.push(env._id);
        });
        applications[app._id] = environment;
        appIds.push(app._id);
    });

    includedCollections.forEach(function (collname) {
        if (excludedCollections.indexOf(collname) !== -1) {
            print("Skip cleanup for excluded collection :" + collname);
            return;
        }

        print("\nCleanup started for collection: " + collname);

        var countOfDocumentToBeDeleted = db[collname].find({appId: {$nin: appIds}}).count();

        print("\nNumber of document to be deleted " + countOfDocumentToBeDeleted);

        var deleteResult = db[collname].deleteMany({appId: {$nin: appIds}});

        print("\nResult of deleted operation " + JSON.stringify(deleteResult));
        print("\nCleanup completed for collection: " + collname);
    });
    print("\nDB cleanup ended!!!");
}

DBCleanup();
