package io.harness.delegate.beans.connector.k8Connector;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class KubernetesAuthCredentialDTO implements DecryptableEntity {}
