package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.annotation.EncryptableSetting;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class GitAuthenticationDTO implements EncryptableSetting {
  public abstract String getUrl();

  public abstract GitConnectionType getGitConnectionType();
}
