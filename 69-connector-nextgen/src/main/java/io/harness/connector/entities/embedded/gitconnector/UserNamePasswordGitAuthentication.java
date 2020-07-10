package io.harness.connector.entities.embedded.gitconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("userNamePassword")
public class UserNamePasswordGitAuthentication implements GitAuthentication {
  String userName;
  String passwordReference;
}
