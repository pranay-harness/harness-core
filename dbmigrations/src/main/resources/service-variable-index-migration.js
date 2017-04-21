//mongeez formatted javascript
//changeset peeyushaggarwal:service-variable-index-migration

if(db.serviceVariables.getIndexes().map(function(index) {  return index.name; }).indexOf("entityId_1_templateId_1_overrideType_1_instances_1_expression_1_type_1_name_1") > -1) {
  db.serviceVariables.dropIndex('entityId_1_templateId_1_overrideType_1_instances_1_expression_1_type_1_name_1');
}

if(db.serviceVariables.getIndexes().map(function(index) {  return index.name; }).indexOf("entityId_1_templateId_1_type_1_name_1") > -1) {
  db.serviceVariables.dropIndex('entityId_1_templateId_1_type_1_name_1');
}
