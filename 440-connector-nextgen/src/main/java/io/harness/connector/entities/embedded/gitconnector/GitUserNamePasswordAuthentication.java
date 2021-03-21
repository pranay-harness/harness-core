package io.harness.connector.entities.embedded.gitconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication")
@OwnedBy(DX)
public class GitUserNamePasswordAuthentication implements GitAuthentication {
  String userName;
  String userNameRef;
  String passwordReference;
}
