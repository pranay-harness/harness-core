package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({ @JsonSubTypes.Type(value = AwsManualConfigSpecDTO.class, name = AwsConstants.MANUAL_CONFIG) })
@ApiModel("AwsCredentialSpec")
@Schema(name = "AwsCredentialSpec", description = "This contains AWS connector credential spec")
public interface AwsCredentialSpecDTO {}
