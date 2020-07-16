package io.harness.delegate.beans.connector.k8Connector;

import lombok.Data;
import software.wings.annotation.EncryptableSetting;

@Data
public abstract class KubernetesAuthCredentialDTO implements EncryptableSetting {}
