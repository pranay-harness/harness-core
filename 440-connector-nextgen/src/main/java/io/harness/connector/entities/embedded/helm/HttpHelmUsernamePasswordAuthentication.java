package io.harness.connector.entities.embedded.helm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "HttpHelmAuthenticationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.helm.HttpHelmUsernamePasswordAuthentication")
public class HttpHelmUsernamePasswordAuthentication implements HttpHelmAuthentication {
  String username;
  String usernameRef;
  String passwordRef;
}
