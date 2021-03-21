package io.harness.connector.entities.embedded.helm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(DX)
public class HttpHelmUsernamePasswordAuthentication implements HttpHelmAuthentication {
  String username;
  String usernameRef;
  String passwordRef;
}
