//mongeez formatted javascript
//changeset peeyushaggarwal:setting-attribute-index-migration


if(db.settingAttributes.getIndexes().map(function(index) {  return index.name; }).indexOf("appId_1_envId_1_name_1") > -1) {
  db.serviceVariables.dropIndex('appId_1_envId_1_name_1');
}

