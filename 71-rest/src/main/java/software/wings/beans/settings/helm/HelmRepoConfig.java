package software.wings.beans.settings.helm;

import software.wings.annotation.EncryptableSetting;

public interface HelmRepoConfig extends EncryptableSetting {
  String getConnectorId();

  String getRepoName();
}
