package io.harness.deployment;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class InstanceDetails {
  private String hostName;
  private String workloadName;
  private boolean newInstance;
  private Map<String, Object> properties;
  private String serviceTemplateName;
  private String serviceTemplateId;
  private String serviceName;
  private String serviceId;
  private PCF pcf;
  private AWS aws;
  private HELM helm;
  private InstanceType instanceType;

  public enum InstanceType { PCF, AWS, HELM }

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
    private String containerId;
    private String containerInstanceId;
    private String containerInstanceArn;
  }

  @Value
  @Builder
  public static class HELM {
    private String ip;
    private String podName;
    private String dockerId;
  }
}
