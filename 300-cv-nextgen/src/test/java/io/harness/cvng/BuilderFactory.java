package io.harness.cvng;

import static io.harness.cvng.core.utils.DateTimeUtils.roundDownToMinBoundary;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.harness.beans.ParameterField;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.MonitoredServiceDataSourceType;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.cdng.beans.CVNGStepInfo;
import io.harness.cvng.cdng.beans.CVNGStepInfo.CVNGStepInfoBuilder;
import io.harness.cvng.cdng.beans.TestVerificationJobSpec;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskBuilder;
import io.harness.cvng.cdng.entities.CVNGStepTask.Status;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.MonitoredServiceDTOBuilder;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceSpec;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.AppDynamicsCVConfig.AppDynamicsCVConfigBuilder;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.cvng.core.entities.NewRelicCVConfig.NewRelicCVConfigBuilder;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.entities.PrometheusCVConfig.PrometheusCVConfigBuilder;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig.SplunkCVConfigBuilder;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.entities.StackdriverCVConfig.StackdriverCVConfigBuilder;
import io.harness.cvng.core.entities.StackdriverLogCVConfig;
import io.harness.cvng.core.entities.StackdriverLogCVConfig.StackdriverLogCVConfigBuilder;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapBuilder;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO.EnvironmentResponseDTOBuilder;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO.ServiceResponseDTOBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

@Data
@Builder(buildMethodName = "unsafeBuild")
public class BuilderFactory {
  public static final String CONNECTOR_IDENTIFIER = "connectorIdentifier";
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

  public CVNGStepTaskBuilder cvngStepTaskBuilder() {
    return CVNGStepTask.builder()
        .accountId(context.getAccountId())
        .activityId(generateUuid())
        .status(Status.IN_PROGRESS)
        .callbackId(generateUuid());
  }

  public ServiceResponseDTOBuilder serviceResponseDTOBuilder() {
    return ServiceResponseDTO.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .identifier(context.getServiceIdentifier())
        .projectIdentifier(context.getProjectIdentifier());
  }

  public EnvironmentResponseDTOBuilder environmentResponseDTOBuilder() {
    return EnvironmentResponseDTO.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .identifier(context.getEnvIdentifier())
        .projectIdentifier(context.getProjectIdentifier());
  }

  public VerificationJobInstanceBuilder verificationJobInstanceBuilder() {
    return VerificationJobInstance.builder()
        .accountId(context.getAccountId())
        .deploymentStartTime(clock.instant().minus(Duration.ofMinutes(2)))
        .startTime(clock.instant())
        .dataCollectionDelay(Duration.ofMinutes(2))
        .resolvedJob(getVerificationJob());
  }

  public MonitoredServiceDTOBuilder monitoredServiceDTOBuilder() {
    return MonitoredServiceDTO.builder()
        .identifier(context.serviceIdentifier + "_" + context.getEnvIdentifier())
        .name("monitored service name")
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .type(MonitoredServiceType.APPLICATION)
        .description(generateUuid())
        .serviceRef(context.getServiceIdentifier())
        .environmentRef(context.getEnvIdentifier())
        .sources(MonitoredServiceDTO.Sources.builder()
                     .healthSources(Arrays.asList(createHealthSource()).stream().collect(Collectors.toSet()))
                     .build());
  }

  public HeatMapBuilder heatMapBuilder() {
    Instant bucketEndTime = clock.instant();
    bucketEndTime = roundDownToMinBoundary(bucketEndTime, 30);
    Instant bucketStartTime = bucketEndTime.minus(24, ChronoUnit.HOURS);
    List<HeatMapRisk> heatMapRisks = new ArrayList<>();

    for (Instant startTime = bucketStartTime; startTime.isBefore(bucketEndTime);
         startTime = startTime.plus(30, ChronoUnit.MINUTES)) {
      heatMapRisks.add(HeatMapRisk.builder()
                           .riskScore(-1)
                           .startTime(startTime)
                           .endTime(startTime.plus(30, ChronoUnit.MINUTES))
                           .build());
    }

    return HeatMap.builder()
        .accountId(context.getAccountId())
        .projectIdentifier(context.getProjectIdentifier())
        .orgIdentifier(context.getOrgIdentifier())
        .category(CVMonitoringCategory.ERRORS)
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier())
        .heatMapResolution(HeatMapResolution.THIRTY_MINUTES)
        .heatMapBucketStartTime(bucketStartTime)
        .heatMapBucketEndTime(bucketEndTime)
        .heatMapRisks(heatMapRisks);
  }

  private HealthSource createHealthSource() {
    return HealthSource.builder()
        .identifier("healthSourceIdentifier")
        .name("health source name")
        .type(MonitoredServiceDataSourceType.APP_DYNAMICS)
        .spec(createHealthSourceSpec())
        .build();
  }

  private HealthSourceSpec createHealthSourceSpec() {
    return AppDynamicsHealthSourceSpec.builder()
        .applicationName("appApplicationName")
        .tierName("tier")
        .connectorRef(CONNECTOR_IDENTIFIER)
        .feature("Application Monitoring")
        .metricPacks(new HashSet<MetricPackDTO>() {
          { add(MetricPackDTO.builder().identifier(CVMonitoringCategory.ERRORS).build()); }
        })
        .build();
  }

  public CVNGStepInfoBuilder cvngStepInfoBuilder() {
    return CVNGStepInfo.builder()
        .type("LoadTest")
        .spec(TestVerificationJobSpec.builder()
                  .duration(ParameterField.createValueField("5m"))
                  .deploymentTag(ParameterField.createValueField("build#1"))
                  .sensitivity(ParameterField.createValueField("Low"))
                  .build());
  }

  public AppDynamicsCVConfigBuilder appDynamicsCVConfigBuilder() {
    return AppDynamicsCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier())
        .identifier(generateUuid())
        .monitoringSourceName(generateUuid())
        .metricPack(MetricPack.builder().build())
        .applicationName(generateUuid())
        .tierName(generateUuid())
        .connectorIdentifier("AppDynamics Connector")
        .category(CVMonitoringCategory.PERFORMANCE)
        .productName(generateUuid());
  }

  public StackdriverLogCVConfigBuilder stackdriverLogCVConfigBuilder() {
    return StackdriverLogCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier())
        .queryName(randomAlphabetic(10))
        .query(randomAlphabetic(10))
        .messageIdentifier(randomAlphabetic(10))
        .serviceInstanceIdentifier(randomAlphabetic(10))
        .identifier(generateUuid())
        .monitoringSourceName(generateUuid())
        .connectorIdentifier("StackdriverLog Connector")
        .category(CVMonitoringCategory.ERRORS)
        .productName(generateUuid());
  }

  public NewRelicCVConfigBuilder newRelicCVConfigBuilder() {
    return NewRelicCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier());
  }

  public PrometheusCVConfigBuilder prometheusCVConfigBuilder() {
    return PrometheusCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier())
        .connectorIdentifier("connectorRef")
        .category(CVMonitoringCategory.PERFORMANCE);
  }

  public StackdriverCVConfigBuilder stackdriverMetricCVConfigBuilder() {
    return StackdriverCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .envIdentifier(context.getEnvIdentifier())
        .connectorIdentifier("connectorRef")
        .dashboardName("dashboardName")
        .category(CVMonitoringCategory.PERFORMANCE);
  }

  public SplunkCVConfigBuilder splunkCVConfigBuilder() {
    return SplunkCVConfig.builder()
        .accountId(context.getAccountId())
        .orgIdentifier(context.getOrgIdentifier())
        .projectIdentifier(context.getProjectIdentifier())
        .serviceIdentifier(context.getServiceIdentifier())
        .serviceInstanceIdentifier(randomAlphabetic(10))
        .envIdentifier(context.getEnvIdentifier())
        .queryName(randomAlphabetic(10))
        .query(randomAlphabetic(10))
        .serviceInstanceIdentifier(randomAlphabetic(10))
        .identifier(generateUuid())
        .monitoringSourceName(generateUuid())
        .connectorIdentifier("Splunk Connector")
        .category(CVMonitoringCategory.ERRORS)
        .productName(generateUuid());
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
