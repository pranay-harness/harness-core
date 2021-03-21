package io.harness.connector.entities.embedded.artifactoryconnector;

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
@FieldNameConstants(innerTypeName = "ArtifactoryAuthenticationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication")
@OwnedBy(DX)
public class ArtifactoryUserNamePasswordAuthentication implements ArtifactoryAuthentication {
  String username;
  String usernameRef;
  String passwordRef;
}
