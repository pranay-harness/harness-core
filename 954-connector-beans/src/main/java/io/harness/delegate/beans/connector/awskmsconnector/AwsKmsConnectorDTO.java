package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.connector.awskmsconnector.AwsKmsCredentialType.MANUAL_CONFIG;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@OwnedBy(PL)
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwsKmsConnectorDTO extends ConnectorConfigDTO {
  @Valid AwsKmsConnectorCredentialDTO credential;

  @SecretReference @ApiModelProperty(dataType = "string") @NotNull SecretRefData kmsArn;
  private String region;
  private boolean isDefault;
  @JsonIgnore private boolean harnessManaged;

  @Builder
  public AwsKmsConnectorDTO(
      SecretRefData kmsArn, String region, AwsKmsConnectorCredentialDTO credential, boolean isDefault) {
    this.kmsArn = kmsArn;
    this.region = region;
    this.credential = credential;
    this.isDefault = isDefault;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    decryptableEntities.add(this);
    if (credential.getCredentialType() == MANUAL_CONFIG) {
      AwsKmsCredentialSpecManualConfigDTO awsKmsManualCredentials =
          (AwsKmsCredentialSpecManualConfigDTO) credential.getConfig();
      decryptableEntities.add(awsKmsManualCredentials);
    }
    return decryptableEntities;
  }

  @Override
  public void validate() {
    Preconditions.checkNotNull(this.kmsArn, "Key Arn cannot be empty");
    Preconditions.checkNotNull(this.region, "Region cannot be empty");
    Preconditions.checkNotNull(this.credential, "credential cannot be empty");

    AwsKmsCredentialType credentialType = this.credential.getCredentialType();
    switch (credentialType) {
      case MANUAL_CONFIG:
        validateManualConfig((AwsKmsCredentialSpecManualConfigDTO) credential.getConfig());
        break;
      case ASSUME_IAM_ROLE:
        validateIAMConfig((AwsKmsCredentialSpecAssumeIAMDTO) credential.getConfig());
        break;
      case ASSUME_STS_ROLE:
        validateSTSConfig((AwsKmsCredentialSpecAssumeSTSDTO) credential.getConfig());
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.", INVALID_REQUEST, USER);
    }
  }

  private void validateSTSConfig(AwsKmsCredentialSpecAssumeSTSDTO config) {
    if (isEmpty(config.getDelegateSelectors())) {
      throw new InvalidRequestException("DelegateSelectors cannot be Empty.", INVALID_REQUEST, USER);
    }
    if (isEmpty(config.getRoleArn())) {
      throw new InvalidRequestException("Role Arn cannot be Empty.", INVALID_REQUEST, USER);
    }
  }

  private void validateIAMConfig(AwsKmsCredentialSpecAssumeIAMDTO config) {
    if (isEmpty(config.getDelegateSelectors())) {
      throw new InvalidRequestException("DelegateSelectors cannot be Empty.", INVALID_REQUEST, USER);
    }
  }

  private void validateManualConfig(AwsKmsCredentialSpecManualConfigDTO config) {
    if (isEmpty(config.getAccessKey().getIdentifier())) {
      throw new InvalidRequestException("Access key cannot be empty.", INVALID_REQUEST, USER);
    }
    if (isEmpty(config.getSecretKey().getIdentifier())) {
      throw new InvalidRequestException("Secret key cannot be empty.", INVALID_REQUEST, USER);
    }
  }
}
