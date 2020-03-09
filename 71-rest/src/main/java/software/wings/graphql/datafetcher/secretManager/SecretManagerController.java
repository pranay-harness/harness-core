package software.wings.graphql.datafetcher.secretManager;

import lombok.experimental.UtilityClass;
import software.wings.beans.SecretManagerConfig;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerBuilder;

@UtilityClass
public class SecretManagerController {
  public QLSecretManagerBuilder populateSecretManager(
      SecretManagerConfig secretManager, QLSecretManagerBuilder builder) {
    return builder.id(secretManager.getUuid()).name(secretManager.getName());
  }
}
