package io.harness.delegate.beans.connector.nexusconnector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@ApiModel("NexusAuthCredentials")
public interface NexusAuthCredentialsDTO extends DecryptableEntity {}
