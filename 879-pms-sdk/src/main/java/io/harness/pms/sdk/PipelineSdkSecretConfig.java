package io.harness.pms.sdk;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PipelineSdkSecretConfig {
  String jwtIdentitySecret;
  String identitySecret;
  String pipelineServiceSecret;
}
