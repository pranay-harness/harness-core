package io.harness.secretmanagerclient.dto.awskms;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConstants;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(PL)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsKmsStsCredentialConfig.class, name = AwsKmsConstants.ASSUME_STS_ROLE)
  , @JsonSubTypes.Type(value = AwsKmsIamCredentialConfig.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
      @JsonSubTypes.Type(value = AwsKmsManualCredentialConfig.class, name = AwsKmsConstants.MANUAL_CONFIG)
})
public interface AwsKmsCredentialSpecConfig {}
