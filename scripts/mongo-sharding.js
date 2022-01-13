/*
 * Copyright 2017 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

// start mongo client and run following command -> 'load("scripts/mongo-sharding.js");'
const conn = new Mongo('cluster0-shard-00-00-w7uwj.mongodb.net:37017');
const db = conn.getDB('admin');
if (db.auth('dbadmin', 'W!ngs@tl@s')) {

  // Sharding already enabled on Atlas prod
  //printjson(db.runCommand({enableSharding: 'harness'}));

  // Shard chunks by file_id
  db.runCommand({shardCollection: 'harness.artifacts.chunks', key: {files_id: 1}});
  db.runCommand({shardCollection: 'harness.audits.chunks', key: {files_id: 1}});
  db.runCommand({shardCollection: 'harness.configs.chunks', key: {files_id: 1}});

  // Shard by appId
  db.runCommand({shardCollection: 'harness.activities', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.artifacts', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.commandLogs', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.pipelineExecutions', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.serviceInstance', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.stateExecutionInstances', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.stateMachines', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.workflowExecutions', key: {appId: 'hashed'}});
  db.runCommand({shardCollection: 'harness.workflows', key: {appId: 'hashed'}});

  // **** figure something out, like adding accountId to audits ****
  //db.runCommand({shardCollection: 'harness.audits', key: {_id: 'hashed'}});

  // Don't shard for now
  // db.runCommand({shardCollection: 'harness.authTokens', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.accounts', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.licenses', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.locks', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.notifyResponses', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.quartz_jobs', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.quartz_locks', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.users', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.waitInstanceErrors', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.waitInstances', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.waitQueues', key: {_id: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.emailVerificationTokens', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.userInvites', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.systemCatalogs', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.notificationBatch', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.notificationGroups', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.notifications', key: {appId: 'hashed'}});
  // artifacts.files
  // audits.files
  // collectorQueue
  // configs.files
  // emailQueue
  // notifyQueue
  // quartz_calendars
  // quartz_schedulers
  // quartz_triggers

  // Shard by accountId later
  // db.runCommand({shardCollection: 'harness.appContainers', key: {accountId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.appdynamicsMetrics', key: {accountId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.delegates', key: {accountId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.delegateTasks', key: {accountId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.settingAttributes', key: {accountId: 'hashed'}});

  // Shard by appId later
  // db.runCommand({shardCollection: 'harness.infrastructureMapping', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.applications', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.artifactStream', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.commands', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.configFiles', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.containerTasks', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.entityVersions', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.environments', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.executionInterrupts', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.hosts', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.pipelines', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.roles', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.serviceCommands', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.serviceTemplates', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.serviceVariables', key: {appId: 'hashed'}});
  // db.runCommand({shardCollection: 'harness.services', key: {appId: 'hashed'}});
} else {
  print('Could not authenticate');
}
