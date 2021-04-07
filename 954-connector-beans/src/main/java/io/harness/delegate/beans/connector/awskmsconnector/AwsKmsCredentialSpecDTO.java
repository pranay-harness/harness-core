package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.harness.beans.DecryptableEntity;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({@JsonSubTypes.Type(value = AwsKmsCredentialSpecManualConfigDTO.class, name = AwsKmsConstants.MANUAL_CONFIG),
        @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeIAMDTO.class, name = AwsKmsConstants.ASSUME_IAM_ROLE),
        @JsonSubTypes.Type(value = AwsKmsCredentialSpecAssumeSTSDTO.class, name = AwsKmsConstants.ASSUME_STS_ROLE)})
@ApiModel("AwsKmsCredentialSpec")
public interface AwsKmsCredentialSpecDTO extends DecryptableEntity {
}
