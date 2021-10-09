package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsSMCredentialSpecManualConfigDTO.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
  , @JsonSubTypes.Type(value = AwsSMCredentialSpecAssumeIAMDTO.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(
          value = AwsSMCredentialSpecAssumeSTSDTO.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
})
@ApiModel("AwsSecretManagerCredentialSpec")
public interface AwsSecretManagerCredentialSpecDTO extends DecryptableEntity {}
