package io.harness.cistatus.service.gitlab;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "personalAccessToken")
public class GitlabConfig {
  String personalAccessToken;
  @NotNull String gitlabUrl;
}
