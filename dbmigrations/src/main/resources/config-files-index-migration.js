//mongeez formatted javascript
//changeset peeyushaggarwal:config-file-index-migration

db.configFiles.dropIndex('entityId_1_templateId_1_relativeFilePath_1_overrideType_1_instances_1_expression_1');
db.configFiles.dropIndex('entityId_1_templateId_1_relativeFilePath_1');

