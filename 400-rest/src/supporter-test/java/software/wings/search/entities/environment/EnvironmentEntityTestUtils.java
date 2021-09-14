/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.entities.environment;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeEvent.ChangeEventBuilder;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.beans.Environment;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class EnvironmentEntityTestUtils {
  public static Environment createEnvironment(String accountId, String appId, String envId, String envName) {
    Environment environment = new Environment();
    environment.setUuid(envId);
    environment.setAppId(appId);
    environment.setAccountId(accountId);
    environment.setName(envName);
    return environment;
  }

  private static DBObject getEnvironmentChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    basicDBObject.put("appId", "appId");

    return basicDBObject;
  }

  public static ChangeEvent createEnvironmentChangeEvent(Environment environment, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(environment)
                             .token("token")
                             .uuid(environment.getUuid())
                             .entityType(Environment.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getEnvironmentChanges());
    }

    return changeEventBuilder.build();
  }
}
