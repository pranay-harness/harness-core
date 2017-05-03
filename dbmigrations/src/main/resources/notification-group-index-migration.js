//mongeez formatted javascript
//changeset srinivas:notification-group-index-migration



if(db.notificationGroups.getIndexes().map(function(index) {  return index.name; }).indexOf("appId_1_name_1") > -1) {
    //Dropping indexes
    db.notificationGroups.dropIndex('appId_1_name_1');
}

