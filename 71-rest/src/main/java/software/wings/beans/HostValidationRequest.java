package software.wings.beans;

import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 2/23/17.
 */
public class HostValidationRequest {
  @NotEmpty private String appId;
  @NotEmpty private String envId;
  @NotEmpty private String computeProviderSettingId;
  @NotEmpty private String deploymentType;
  @NotEmpty private String hostConnectionAttrs;
  private ExecutionCredential executionCredential;
  private List<String> hostNames = new ArrayList<>();

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getComputeProviderSettingId() {
    return computeProviderSettingId;
  }

  public void setComputeProviderSettingId(String computeProviderSettingId) {
    this.computeProviderSettingId = computeProviderSettingId;
  }

  public String getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  public ExecutionCredential getExecutionCredential() {
    return executionCredential;
  }

  public void setExecutionCredential(ExecutionCredential executionCredential) {
    this.executionCredential = executionCredential;
  }

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }
}
