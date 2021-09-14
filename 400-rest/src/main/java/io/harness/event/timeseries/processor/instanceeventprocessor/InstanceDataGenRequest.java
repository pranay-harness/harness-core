/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
