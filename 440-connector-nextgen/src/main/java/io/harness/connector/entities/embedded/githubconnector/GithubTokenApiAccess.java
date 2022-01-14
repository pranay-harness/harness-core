package io.harness.connector.entities.embedded.githubconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubTokenApiAccess")
public class GithubTokenApiAccess implements GithubApiAccess {
  String tokenRef;
}
