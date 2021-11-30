package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsCredentialSpecManualConfigDTO.class, name = AwsKmsConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeIAMDTO.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeSTSDTO.class, name = AwsKmsConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsKmsCredentialSpec")
@Schema(name = "AwsKmsCredentialSpec", description = "This contains the credential spec of AWS KMS SM")
public interface AwsKmsCredentialSpecDTO extends DecryptableEntity {}
