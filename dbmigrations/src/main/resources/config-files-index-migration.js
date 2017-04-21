//mongeez formatted javascript
//changeset peeyushaggarwal:config-file-index-migration


if(db.configFiles.getIndexes().map(function(index) {  return index.name; }).indexOf("entityId_1_templateId_1_relativeFilePath_1_overrideType_1_instances_1_expression_1") > -1) {
  db.configFiles.dropIndex('entityId_1_templateId_1_relativeFilePath_1_overrideType_1_instances_1_expression_1');
}

if(db.configFiles.getIndexes().map(function(index) {  return index.name; }).indexOf("entityId_1_templateId_1_relativeFilePath_1") > -1) {
  db.configFiles.dropIndex('entityId_1_templateId_1_relativeFilePath_1');
}
