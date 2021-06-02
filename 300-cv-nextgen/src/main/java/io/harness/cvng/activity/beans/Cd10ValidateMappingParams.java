package io.harness.cvng.activity.beans;

import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Cd10ValidateMappingParams {
  @NotNull String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String activitySourceIdentifier;
  @NotNull String cd10AppId;
  @NotNull String cd10ServiceId;
  @NotNull String cd10EnvId;
  @NotNull RuntimeParameter serviceIdentifier;
  @NotNull RuntimeParameter environmentIdentifier;
}
