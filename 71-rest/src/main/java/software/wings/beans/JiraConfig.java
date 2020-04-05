package software.wings.beans;

import static software.wings.audit.ResourceType.COLLABORATION_PROVIDER;
import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CollaborationProviderYaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonTypeName("JIRA")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class JiraConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  public enum JiraSetupType { JIRA_CLOUD, JIRA_SERVER }
  private static final CharSequence JIRA_CLOUD_DOMAINNAME = ".atlassian.net";

  @Attributes(title = "Base URL", required = true) @NotEmpty private String baseUrl;

  @Attributes(title = "Username", required = true) @NotEmpty private String username;

  /**
   * Handles both password & OAuth(1.0) token.
   */
  @Attributes(title = "Password/API Token", required = true)
  @Encrypted(fieldName = "password/api_token")
  private char[] password;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore @NotEmpty private String accountId;

  public JiraConfig() {
    super(SettingVariableTypes.JIRA.name());
  }

  public JiraConfig(String baseUrl, String username, char[] password, String encryptedPassword, String accountId) {
    this();
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = Arrays.copyOf(password, password.length);
    this.encryptedPassword = encryptedPassword;
    this.accountId = accountId;
  }

  private JiraSetupType getSetupType() {
    JiraSetupType setupType = null;
    if (StringUtils.isNotEmpty(baseUrl)) {
      setupType = baseUrl.contains(JIRA_CLOUD_DOMAINNAME) ? JiraSetupType.JIRA_CLOUD : JiraSetupType.JIRA_SERVER;
    }

    return setupType;
  }

  @Override
  public String fetchResourceCategory() {
    return COLLABORATION_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CollaborationProviderYaml {
    private String baseUrl;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;

    @Builder
    public Yaml(String type, String harnessApiVersion, String baseUrl, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.baseUrl = baseUrl;
      this.username = username;
      this.password = password;
    }
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(baseUrl));
  }
}
