package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 8/4/16.
 */
@JsonTypeName("APP_DYNAMICS")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class AppDynamicsConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Account Name", required = true) @NotEmpty private String accountname;
  @Attributes(title = "Password", required = true) @Encrypted(fieldName = "password") private char[] password;
  @Attributes(title = "Controller URL", required = true) @NotEmpty private String controllerUrl;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new App dynamics config.
   */
  public AppDynamicsConfig() {
    super(StateType.APP_DYNAMICS.name());
  }

  private AppDynamicsConfig(String username, String accountname, char[] password, String controllerUrl,
      String accountId, String encryptedPassword) {
    this();
    this.username = username;
    this.accountname = accountname;
    this.password = password;
    this.controllerUrl = controllerUrl;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(controllerUrl));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String username;
    private String password;
    private String accountName;
    private String controllerUrl;

    @Builder
    public Yaml(String type, String harnessApiVersion, String username, String password, String accountName,
        String controllerUrl, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.username = username;
      this.password = password;
      this.accountName = accountName;
      this.controllerUrl = controllerUrl;
    }
  }
}
