/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.entities.application;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeEvent.ChangeEventBuilder;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.beans.Application;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ApplicationEntityTestUtils {
  public static Application createApplication(String accountId, String appId, String appName) {
    Application application = new Application();
    application.setName(appName);
    application.setAccountId(accountId);
    application.setUuid(appId);
    return application;
  }

  public static DBObject getApplicationChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    return basicDBObject;
  }

  public static ChangeEvent createApplicationChangeEvent(Application application, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(application)
                             .token("token")
                             .uuid(application.getUuid())
                             .entityType(Application.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getApplicationChanges());
    }
    return changeEventBuilder.build();
  }
}
