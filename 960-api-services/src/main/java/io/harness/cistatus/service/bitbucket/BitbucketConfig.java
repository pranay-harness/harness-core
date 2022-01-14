package io.harness.cistatus.service.bitbucket;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "personalAccessToken")
public class BitbucketConfig {
  String personalAccessToken;
  @NotNull String bitbucketUrl;
}
