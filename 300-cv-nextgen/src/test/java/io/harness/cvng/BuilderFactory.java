package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.AppDynamicsCVConfig.AppDynamicsCVConfigBuilder;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
@Data
@Builder(buildMethodName = "unsafeBuild")
public class BuilderFactory {
  @Getter @Setter(AccessLevel.PRIVATE) private Clock clock;
  @Getter @Setter(AccessLevel.PRIVATE) private Context context;
  public static class BuilderFactoryBuilder {
    public BuilderFactory build() {
      BuilderFactory builder = unsafeBuild();
      if (builder.clock == null) {
        builder.setClock(Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC));
      }
      if (builder.getContext() == null) {
        builder.setContext(Context.defaultContext());
      }
      return builder;
    }
  }
  public VerificationJobInstanceBuilder verificationJobInstanceBuilder() {
    return VerificationJobInstance.builder()
        .accountId(context.getAccountId())
        .deploymentStartTime(clock.instant().minus(Duration.ofMinutes(2)))
        .startTime(clock.instant())
        .dataCollectionDelay(Duration.ofMinutes(2))
        .resolvedJob(getVerificationJob());
  }

  public AppDynamicsCVConfigBuilder appDynamicsCVConfigBuilder() {
    return AppDynamicsCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier());
  }

  private VerificationJob getVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    testVerificationJob.setAccountId(context.getAccountId());
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setMonitoringSources(Arrays.asList("monitoringIdentifier"));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(context.getServiceIdentifier(), false);
    testVerificationJob.setEnvIdentifier(context.getEnvIdentifier(), false);
    testVerificationJob.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJob.setDuration(Duration.ofMinutes(5));
    testVerificationJob.setProjectIdentifier(context.getProjectIdentifier());
    testVerificationJob.setOrgIdentifier(context.getOrgIdentifier());
    return testVerificationJob;
  }

  public static BuilderFactory getDefault() {
    return BuilderFactory.builder().build();
  }
  @Value
  @Builder
  public static class Context {
    String accountId;
    String orgIdentifier;
    String projectIdentifier;
    String serviceIdentifier;
    String envIdentifier;
    public static Context defaultContext() {
      return Context.builder()
          .accountId(generateUuid())
          .orgIdentifier(generateUuid())
          .projectIdentifier(generateUuid())
          .envIdentifier(generateUuid())
          .serviceIdentifier(generateUuid())
          .build();
    }
  }
}
