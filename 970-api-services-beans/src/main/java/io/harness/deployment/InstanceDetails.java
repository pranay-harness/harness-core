/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.deployment;

import com.amazonaws.services.ec2.model.Instance;
import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class InstanceDetails {
  private String hostName;
  private String workloadName;
  @NonFinal @Setter private boolean newInstance;
  private Map<String, Object> properties;
  private String serviceTemplateName;
  @NonFinal @Setter private String serviceTemplateId;
  private String serviceName;
  private String serviceId;
  private PCF pcf;
  private AWS aws;
  private PHYSICAL_HOST physicalHost;
  private K8s k8s;
  private AZURE_VMSS azureVmss;
  private AZURE_WEBAPP azureWebapp;
  private InstanceType instanceType;

  public enum InstanceType { PCF, AWS, K8s, PHYSICAL_HOST, AZURE_VMSS, AZURE_WEBAPP }

  @Value
  @Builder
  public static class PCF {
    private String applicationId;
    private String applicationName;
    private String instanceIndex;
  }

  @Value
  @Builder
  public static class AWS {
    private Instance ec2Instance;
    private String ip;
    private String instanceId;
    private String publicDns;
    private String taskId;
    private String taskArn;
    private String dockerId;
    private String completeDockerId;
    private String containerId;
    private String containerInstanceId;
    private String containerInstanceArn;
    private String ecsServiceName;
  }

  @Value
  @Builder
  public static class K8s {
    private String ip;
    private String podName;
    private String dockerId;
  }

  @Value
  @Builder
  public static class PHYSICAL_HOST {
    private String publicDns;
    private String instanceId;
  }

  @Value
  @Builder
  public static class AZURE_VMSS {
    private String ip;
    private String instanceId;
    private String publicDns;
  }

  @Value
  @Builder
  public static class AZURE_WEBAPP {
    private String ip;
    private String instanceId;
    private String instanceType;
    private String instanceName;
    private String instanceHostName;
    private String appName;
    private String deploySlot;
    private String deploySlotId;
    private String appServicePlanId;
  }
}
