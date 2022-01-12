/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
