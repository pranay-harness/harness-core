package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@ApiModel("AwsCredentialSpec")
public interface AwsCredentialSpecDTO {}
