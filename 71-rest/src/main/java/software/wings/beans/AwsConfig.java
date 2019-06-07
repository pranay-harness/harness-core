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
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 12/27/16.
 */
@JsonTypeName("AWS")
@Data
@Builder
@ToString(exclude = "secretKey")
@EqualsAndHashCode(callSuper = false)
public class AwsConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  private static final String AWS_URL = "https://aws.amazon.com/";
  @Attributes(title = "Access Key") private String accessKey;
  @Attributes(title = "Secret Key") @Encrypted private char[] secretKey;
  @SchemaIgnore @NotEmpty private String accountId; // internal
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;

  @Attributes(title = "Use Ec2 Iam role") private boolean useEc2IamCredentials;
  @Attributes(title = "Ec2 Iam role tags") private String tag;

  /**
   * Instantiates a new Aws config.
   */
  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  public AwsConfig(String accessKey, char[] secretKey, String accountId, String encryptedSecretKey,
      boolean useEc2IamCredentials, String tag) {
    this();
    this.accessKey = accessKey;
    this.secretKey = secretKey == null ? null : secretKey.clone();
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
    this.useEc2IamCredentials = useEc2IamCredentials;
    this.tag = tag;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String accessKey;
    private String secretKey;
    private boolean useEc2IamCredentials;
    private String tag;

    @Builder
    public Yaml(String type, String harnessApiVersion, String accessKey, String secretKey,
        UsageRestrictions.Yaml usageRestrictions, boolean useEc2IamCredentials, String tag) {
      super(type, harnessApiVersion, usageRestrictions);
      this.accessKey = accessKey;
      this.secretKey = secretKey;
      this.useEc2IamCredentials = useEc2IamCredentials;
      this.tag = tag;
    }
  }
}
