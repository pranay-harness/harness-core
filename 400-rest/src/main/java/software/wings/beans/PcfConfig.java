package software.wings.beans;

import static java.util.Collections.emptyList;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("PCF")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class PcfConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Endpoint URL", required = true) @NotEmpty private String endpointUrl;
  @Attributes(title = "Username", required = true)
  @Encrypted(fieldName = "username", isReference = true)
  private char[] username;
  @Attributes(title = "Password", required = true) @Encrypted(fieldName = "password") private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @Attributes(title = "Use Encrypted Username") private boolean useEncryptedUsername;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedUsername;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  private boolean skipValidation;

  public PcfConfig() {
    super(SettingVariableTypes.PCF.name());
  }

  public PcfConfig(String endpointUrl, char[] username, char[] password, String accountId, boolean useEncryptedUsername,
      String encryptedUsername, String encryptedPassword, boolean skipValidation) {
    this();
    this.endpointUrl = endpointUrl;
    this.username = username == null ? null : username.clone();
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.useEncryptedUsername = useEncryptedUsername;
    this.encryptedUsername = encryptedUsername;
    this.encryptedPassword = encryptedPassword;
    this.skipValidation = skipValidation;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    /*
     * We do not need HTTP capability for end point Url.
     * The PcfConnectivityCapability already checks for decryption and
     * connectivity.
     */
    return emptyList();
  }
}
