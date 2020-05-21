package software.wings.security.encryption.secretsmanagerconfigs;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SecretManagerConfig;

import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "CustomSecretsManagerConfigKeys")
public class CustomSecretsManagerConfig extends SecretManagerConfig {
  @NonNull @NotEmpty @Attributes(title = "Name") private String name;
  @NonNull @NotEmpty @Attributes(title = "Template Shell Script") private String templateId;
  @NonNull @Attributes(title = "Delegate Selectors") private List<String> delegateSelectors;
  @NonNull @Attributes(title = "Test Parameters") private Map<String, String> testParameters;
  @Attributes(title = "Execute on Delegate") private boolean executeOnDelegate;
  @Attributes(title = "Templatize Connector") private boolean isConnectorTemplatized;
  @Attributes(title = "Target Host") private String host;
  @Attributes(title = "Command Path") private String commandPath;
  @Attributes(title = "Connection Attributes Reference Id") private String connectorId;
  @Transient private CustomSecretsManagerShellScript customSecretsManagerShellScript;
  @Transient private EncryptableSetting remoteHostConnector;

  @Override
  public void maskSecrets() {
    // Nothing to mask
  }

  @Override
  public String getEncryptionServiceUrl() {
    return null;
  }

  @Override
  public String getValidationCriteria() {
    return null;
  }
}
