//mongeez formatted javascript
//changeset anubhaw:appstack-index-migration


if(db.appContainers.getIndexes().map(function(index) {  return index.name; }).indexOf("appId_1_name_1") > -1) {
    db.appContainers.dropIndex('appId_1_name_1');
}

