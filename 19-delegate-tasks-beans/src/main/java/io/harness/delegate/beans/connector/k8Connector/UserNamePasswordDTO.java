package io.harness.delegate.beans.connector.k8Connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  String username;
  String password;
  String cacert;
}
