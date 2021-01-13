package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AwsInheritFromDelegateSpecDTO.class, name = AwsConstants.INHERIT_FROM_DELEGATE)
  , @JsonSubTypes.Type(value = AwsManualConfigSpecDTO.class, name = AwsConstants.MANUAL_CONFIG)
})
@ApiModel("AwsCredentialSpec")
public interface AwsCredentialSpecDTO {}
