/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.timeseries.processor.instanceeventprocessor;

import io.harness.event.timeseries.processor.EventProcessor;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class InstanceDataGenRequest {
  Long timestamp;
  String accountId;
  String appId;
  String serviceId;
  String envId;
  String cloudProvider;
  String instanceType;
  String artifactId;
  Integer instanceCount;

  public Map<String, Object> getDataMap() {
    Map<String, Object> dataMap = new HashMap<>();
    dataMap.put(EventProcessor.APPID, appId);
    dataMap.put(EventProcessor.SERVICEID, serviceId);
    dataMap.put(EventProcessor.ENVID, envId);
    dataMap.put(EventProcessor.CLOUDPROVIDERID, cloudProvider);
    dataMap.put(EventProcessor.INSTANCETYPE, instanceType);
    dataMap.put(EventProcessor.ARTIFACTID, artifactId);
    dataMap.put(EventProcessor.INSTANCECOUNT, instanceCount);

    return dataMap;
  }
}
