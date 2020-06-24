package io.harness.connector.apis.dtos.K8Connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  String username;
  String password;
  String cacert;
}
