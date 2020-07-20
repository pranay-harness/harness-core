package io.harness.delegate.beans.connector.gitconnector;

import software.wings.annotation.EncryptableSetting;

public abstract class GitAuthenticationDTO implements EncryptableSetting {
  public abstract String getUrl();

  public abstract GitConnectionType getGitConnectionType();
}
