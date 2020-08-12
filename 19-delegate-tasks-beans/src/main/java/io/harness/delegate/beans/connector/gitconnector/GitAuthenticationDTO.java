package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.beans.DecryptableEntity;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class GitAuthenticationDTO implements DecryptableEntity {
  public abstract String getUrl();

  public abstract String getBranchName();

  public abstract GitConnectionType getGitConnectionType();
}
