/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.entities.service;

import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeEvent.ChangeEventBuilder;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.beans.Service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ServiceEntityTestUtils {
  public static Service createService(String accountId, String appId, String serviceId, String serviceName) {
    Service service = new Service();
    service.setUuid(serviceId);
    service.setAppId(appId);
    service.setAccountId(accountId);
    service.setName(serviceName);
    service.setK8sV2(true);
    return service;
  }

  private static DBObject getServiceChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    basicDBObject.put("appId", "appId");
    basicDBObject.put("orchestration", "orchestration");
    basicDBObject.put("pipelineStages", "pipelineStages");

    return basicDBObject;
  }

  public static ChangeEvent createServiceChangeEvent(Service service, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(service)
                             .token("token")
                             .uuid(service.getUuid())
                             .entityType(Service.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getServiceChanges());
    }

    return changeEventBuilder.build();
  }
}
