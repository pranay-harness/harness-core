package io.harness.connector.entities.embedded.nexusconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "NexusUserNamePasswordAuthenticationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication")
public class NexusUserNamePasswordAuthentication implements NexusAuthentication {
  String username;
  String usernameRef;
  String passwordRef;
}
