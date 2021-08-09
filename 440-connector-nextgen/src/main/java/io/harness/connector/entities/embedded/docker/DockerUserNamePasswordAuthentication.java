package io.harness.connector.entities.embedded.docker;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "DockerAuthenticationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication")
public class DockerUserNamePasswordAuthentication implements DockerAuthentication {
  String username;
  String usernameRef;
  String passwordRef;
}
