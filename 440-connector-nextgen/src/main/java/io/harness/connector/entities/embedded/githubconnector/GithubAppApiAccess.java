package io.harness.connector.entities.embedded.githubconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.githubconnector.GithubAppApiAccess")
public class GithubAppApiAccess implements GithubApiAccess {
  String installationId;
  String applicationId;
  String privateKeyRef;
}
