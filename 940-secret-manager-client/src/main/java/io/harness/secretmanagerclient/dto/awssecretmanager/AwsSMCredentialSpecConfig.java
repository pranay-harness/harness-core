package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awssecretmanager.AwsSecretManagerConstants;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsIamCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsManualCredentialConfig;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsStsCredentialConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsStsCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_STS_ROLE)
  , @JsonSubTypes.Type(value = AwsKmsIamCredentialConfig.class, name = AwsSecretManagerConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsManualCredentialConfig.class, name = AwsSecretManagerConstants.MANUAL_CONFIG)
})
public interface AwsSMCredentialSpecConfig {}