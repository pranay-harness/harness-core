package io.harness.delegate.task.ci;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class CIBuildStatusPushParameters extends CIBuildPushParameters {
  private String title;
  private String desc;
  private String state;

  @Builder
  public CIBuildStatusPushParameters(String buildNumber, String repo, String owner, String sha, String identifier,
      String target_url, String key, String installId, String appId, String title, String desc, String state) {
    super(buildNumber, repo, owner, sha, identifier, target_url, key, installId, appId);
    this.title = title;
    this.desc = desc;
    this.state = state;
    this.commandType = CIBuildPushTaskType.STATUS;
  }

  @Override
  public CIBuildPushTaskType getCommandType() {
    return commandType;
  }
}
