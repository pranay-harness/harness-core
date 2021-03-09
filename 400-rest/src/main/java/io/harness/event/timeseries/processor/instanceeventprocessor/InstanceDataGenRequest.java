package io.harness.event.timeseries.processor.instanceeventprocessor;

import io.harness.event.timeseries.processor.EventProcessor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

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
