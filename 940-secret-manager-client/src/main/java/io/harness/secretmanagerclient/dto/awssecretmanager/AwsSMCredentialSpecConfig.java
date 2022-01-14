package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsSMStsCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
  , @JsonSubTypes.Type(value = AwsSMIamRoleCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsSMManualCredentialConfig.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
})
public interface AwsSMCredentialSpecConfig {}