package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.beans.DecryptableEntity;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class GitAuthenticationDTO implements DecryptableEntity {
  @NotEmpty public abstract String getUrl();

  public abstract String getBranchName();

  public abstract GitConnectionType getGitConnectionType();
}
