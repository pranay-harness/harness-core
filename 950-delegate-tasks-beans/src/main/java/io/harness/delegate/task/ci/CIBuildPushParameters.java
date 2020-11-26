package io.harness.delegate.task.ci;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public abstract class CIBuildPushParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String buildNumber;
  private String repo;
  private String owner;
  private String sha;
  private String identifier;
  private String target_url;
  private String key; // TODO it will come via github app connector with encrypted details
  private String installId; // TODO it will come via github app connector with encrypted details
  private String appId; //  TODO it will come via github app connector
  private String token; //  TODO it will come via bitbucket/gitlab  connector details
  private String userName; //  TODO it will come via bitbucket/gitlab connector details
  private ConnectorDetails connectorDetails; // Use connectorDetails to retrieve all information
  private GitSCMType gitSCMType;
  @NotEmpty CIBuildPushTaskType commandType;
  public enum CIBuildPushTaskType { STATUS, CHECKS }

  public CIBuildPushParameters(String buildNumber, String repo, String owner, String sha, String identifier,
      String target_url, String key, String installId, String appId, String token, String userName,
      GitSCMType gitSCMType) {
    this.buildNumber = buildNumber;
    this.repo = repo;
    this.owner = owner;
    this.sha = sha;
    this.identifier = identifier;
    this.target_url = target_url;
    this.key = key;
    this.installId = installId;
    this.appId = appId;
    this.gitSCMType = gitSCMType;
    this.token = token;
    this.userName = userName;
  }
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.emptyList();
  }
}
