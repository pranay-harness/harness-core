package io.harness.cistatus.service;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "privateKey")
public class GithubAppConfig {
  String privateKey;
  String appId;
  String installationId;
  String githubUrl;
}
