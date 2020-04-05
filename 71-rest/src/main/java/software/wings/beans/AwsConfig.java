package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.ccm.CCMConfig;
import io.harness.ccm.CloudCostAware;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
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
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.Arrays;
import java.util.List;

@JsonTypeName("AWS")
@Data
@Builder
@ToString(exclude = "secretKey")
@EqualsAndHashCode(callSuper = false)
public class AwsConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  private static final String AWS_URL = "https://aws.amazon.com/";
  @Attributes(title = "Access Key") private String accessKey;
  @Attributes(title = "Secret Key") @Encrypted(fieldName = "secret_key") private char[] secretKey;
  @SchemaIgnore @NotEmpty private String accountId; // internal
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;

  @Attributes(title = "Use Ec2 Iam role") private boolean useEc2IamCredentials;
  @Attributes(title = "Ec2 Iam role tags") private String tag;
  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;
  private boolean assumeCrossAccountRole;
  private AwsCrossAccountAttributes crossAccountAttributes;

  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  public AwsConfig(String accessKey, char[] secretKey, String accountId, String encryptedSecretKey,
      boolean useEc2IamCredentials, String tag, CCMConfig ccmConfig, boolean assumeCrossAccountRole,
      AwsCrossAccountAttributes crossAccountAttributes) {
    this();
    this.accessKey = accessKey;
    this.secretKey = secretKey == null ? null : secretKey.clone();
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
    this.useEc2IamCredentials = useEc2IamCredentials;
    this.tag = tag;
    this.ccmConfig = ccmConfig;
    this.assumeCrossAccountRole = assumeCrossAccountRole;
    this.crossAccountAttributes = crossAccountAttributes;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String accessKey;
    private String secretKey;
    private boolean useEc2IamCredentials;
    private String tag;
    private boolean assumeCrossAccountRole;
    private AwsCrossAccountAttributes crossAccountAttributes;

    @Builder
    public Yaml(String type, String harnessApiVersion, String accessKey, String secretKey,
        UsageRestrictions.Yaml usageRestrictions, boolean useEc2IamCredentials, String tag,
        boolean assumeCrossAccountRole, AwsCrossAccountAttributes crossAccountAttributes) {
      super(type, harnessApiVersion, usageRestrictions);
      this.accessKey = accessKey;
      this.secretKey = secretKey;
      this.useEc2IamCredentials = useEc2IamCredentials;
      this.tag = tag;
      this.assumeCrossAccountRole = assumeCrossAccountRole;
      this.crossAccountAttributes = crossAccountAttributes;
    }
  }
}
