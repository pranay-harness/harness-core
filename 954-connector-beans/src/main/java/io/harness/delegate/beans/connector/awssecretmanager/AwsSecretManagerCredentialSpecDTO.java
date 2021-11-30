package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsSMCredentialSpecManualConfigDTO.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsSMCredentialSpecAssumeIAMDTO.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(
          value = AwsSMCredentialSpecAssumeSTSDTO.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsSecretManagerCredentialSpec")
@Schema(name = "AwsSecretManagerCredentialSpec",
    description = "This contains AWS SM credential spec according to the role using which SM is created")
public interface AwsSecretManagerCredentialSpecDTO extends DecryptableEntity {}
