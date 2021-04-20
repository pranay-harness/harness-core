package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = AwsManualConfigSpecDTO.class, name = AwsConstants.MANUAL_CONFIG) })
@ApiModel("AwsCredentialSpec")
public interface AwsCredentialSpecDTO {}
