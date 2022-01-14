package io.harness.connector.entities.embedded.gitconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication")
public class GitUserNamePasswordAuthentication implements GitAuthentication {
  String userName;
  String userNameRef;
  String passwordReference;
}
