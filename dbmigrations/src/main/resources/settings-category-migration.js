//mongeez formatted javascript
//changeset peeyushaggarwal:settings-category-migration

db.settingAttributes.updateMany({ "value.type": { $in: [ "GCP", "AWS", "KUBERNETES" ]}}, { "$set": { "category": "CLOUD_PROVIDER"}, "$unset": { "isPluginSetting": ""}});
db.settingAttributes.updateMany({ "value.type": { $in: [ "SMTP", "JENKINS", "BAMBOO", "SPLUNK", "APP_DYNAMICS", "SLACK", "DOCKER" ]}}, { "$set": { "category": "CONNECTOR"}, "$unset": { "isPluginSetting": ""}});
db.settingAttributes.updateMany({ "value.type": { $nin: [ "GCP", "AWS", "KUBERNETES", "SMTP", "JENKINS", "BAMBOO", "SPLUNK", "APP_DYNAMICS", "SLACK", "DOCKER" ]}}, { "$set": { "category": "SETTING"}, "$unset": { "isPluginSetting": ""}});
