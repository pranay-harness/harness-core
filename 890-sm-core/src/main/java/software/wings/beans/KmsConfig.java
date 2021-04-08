package software.wings.beans;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.Lists;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsManualCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.BaseAwsKmsConfigDTO;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.harness.beans.SecretManagerCapabilities.*;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.helpers.GlobalSecretManagerUtils.GLOBAL_ACCOUNT_ID;
import static io.harness.security.encryption.SecretManagerType.KMS;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"accessKey", "secretKey", "kmsArn"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "KmsConfigKeys")
public class KmsConfig extends SecretManagerConfig {
  private static final String TASK_SELECTORS = "Task Selectors";
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key") @Encrypted(fieldName = "aws_access_key") private String accessKey;

  @Attributes(title = "AWS Secret Key") @Encrypted(fieldName = "aws_secret_key") private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted(fieldName = "aws_key_arn") private String kmsArn;

  @Attributes(title = "AWS Region", required = true) private String region;

  @Attributes(title = "AWS AssumeIamRole") private boolean assumeIamRoleOnDelegate;
  @Attributes(title = "AWS AssumeStsRole") private boolean assumeStsRoleOnDelegate;
  @Attributes(title = "AWS AssumeStsRoleDuration")
  @Builder.Default
  private int assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
  @Attributes(title = "AWS AssumeStsRoleARN") private String roleArn;
  @Attributes(title = "AWS AssumeStsExternalName") private String externalName;
  @Attributes(title = "AWS DelegateSelectors") private Set<String> delegateSelectors;

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getEncryptionServiceUrl() {
    return KmsUtils.generateKmsUrl(region);
  }

  @JsonIgnore
  @SchemaIgnore
  @Override
  public String getValidationCriteria() {
    return EncryptionType.KMS + "-" + getName() + "-" + getUuid();
  }

  @Override
  public SecretManagerType getType() {
    return KMS;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>(
        Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
            region, maskingEvaluator)));
    if (isNotEmpty(delegateSelectors)) {
      executionCapabilities.add(
          SelectorCapability.builder().selectors(delegateSelectors).selectorOrigin(TASK_SELECTORS).build());
    }
    return executionCapabilities;
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.KMS;
  }

  @Override
  public void maskSecrets() {
    if (isNotEmpty(this.secretKey)) {
      this.secretKey = SECRET_MASK;
    }
    if (isNotEmpty(this.getKmsArn())) {
      this.kmsArn = SECRET_MASK;
    }
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    List<SecretManagerCapabilities> secretManagerCapabilities =
        Lists.newArrayList(CREATE_INLINE_SECRET, CREATE_FILE_SECRET, CAN_BE_DEFAULT_SM);
    if (!isTemplatized()) {
      secretManagerCapabilities.add(TRANSITION_SECRET_FROM_SM);
      secretManagerCapabilities.add(TRANSITION_SECRET_TO_SM);
    }
    return secretManagerCapabilities;
  }

  @Override
  public boolean isGlobalKms() {
    return GLOBAL_ACCOUNT_ID.equals(getAccountId());
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    AwsKmsConfigDTO awsKmsConfigDTO = AwsKmsConfigDTO.builder()
            .name(getName())
            .isDefault(isDefault())
            .encryptionType(getEncryptionType())
            .build();

    BaseAwsKmsConfigDTO baseAwsKmsConfigDTO = populateBaseAwsKmsConfigDTO();

    SecretManagerConfigMapper.updateNGSecretManagerMetadata(getNgMetadata(), awsKmsConfigDTO);
    if (!maskSecrets) {
      baseAwsKmsConfigDTO.setKmsArn(getKmsArn());
      if(isNotEmpty(getAccessKey()) && isNotEmpty(getSecretKey())) {
        AwsKmsManualCredentialConfig manualCredential = (AwsKmsManualCredentialConfig) baseAwsKmsConfigDTO.getCredential();
        manualCredential.setAccessKey(getAccessKey());
        manualCredential.setSecretKey(getSecretKey());
      }
    }
    awsKmsConfigDTO.setBaseAwsKmsConfigDTO(baseAwsKmsConfigDTO);
    return awsKmsConfigDTO;
  }

  private BaseAwsKmsConfigDTO populateBaseAwsKmsConfigDTO() {
    return BaseAwsKmsConfigDTO.builder()
                            .name(getName())
                            .region(getRegion())
                            .build();
  }
}
