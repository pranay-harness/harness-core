package software.wings.beans;

import static software.wings.settings.SettingVariableTypes.GCP;

import static java.util.Collections.emptyList;

import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CloudCostAware;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;

import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CloudProviderYaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
@Data
@Builder
@ToString(exclude = {"serviceAccountKeyFileContent", "encryptedServiceAccountKeyFileContent"})
@EqualsAndHashCode(callSuper = false)
public class GcpConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  @Encrypted(fieldName = "service_account_key_file") private char[] serviceAccountKeyFileContent;

  @SchemaIgnore @NotEmpty private String accountId;
  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedServiceAccountKeyFileContent;

  private boolean useDelegate;
  private String delegateSelector;
  private boolean skipValidation;

  public GcpConfig() {
    super(GCP.name());
  }

  public GcpConfig(char[] serviceAccountKeyFileContent, String accountId, CCMConfig ccmConfig,
      String encryptedServiceAccountKeyFileContent, boolean useDelegate, String delegateSelector,
      boolean skipValidation) {
    this();
    this.serviceAccountKeyFileContent =
        serviceAccountKeyFileContent == null ? null : serviceAccountKeyFileContent.clone();
    this.accountId = accountId;
    this.ccmConfig = ccmConfig;
    this.encryptedServiceAccountKeyFileContent = encryptedServiceAccountKeyFileContent;
    this.delegateSelector = delegateSelector;
    this.useDelegate = useDelegate;
    this.skipValidation = skipValidation;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return emptyList();
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }
}
