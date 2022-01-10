/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
