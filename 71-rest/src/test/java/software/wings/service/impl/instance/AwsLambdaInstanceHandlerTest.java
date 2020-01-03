package software.wings.service.impl.instance;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseStepExecutionData.PhaseStepExecutionDataBuilder;
import software.wings.api.lambda.AwsLambdaDeploymentInfo;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.Tag;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.ServerlessInstance.ServerlessInstanceBuilder;
import software.wings.beans.infrastructure.instance.ServerlessInstanceType;
import software.wings.beans.infrastructure.instance.info.AwsLambdaInstanceInfo;
import software.wings.beans.infrastructure.instance.info.ServerlessInstanceInfo;
import software.wings.beans.infrastructure.instance.key.AwsLambdaInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsLambdaDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.aws.model.embed.AwsLambdaDetails;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;
import software.wings.service.intfc.instance.ServerlessInstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.PhaseStepExecutionSummary;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AwsLambdaInstanceHandlerTest extends WingsBaseTest {
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private AppService appService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;

  @Mock ArtifactService artifactService;
  @Mock ServerlessInstanceService serverlessInstanceService;
  @Mock DelegateProxyFactory delegateProxyFactory;

  @InjectMocks @Inject @Spy AwsLambdaInstanceHandler awsLambdaInstanceHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validateInstanceType_valid_infraMapping() {
    awsLambdaInstanceHandler.validateInstanceType(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validateInstanceType_valid_invalid_infraMapping() {
    awsLambdaInstanceHandler.validateInstanceType(InfrastructureMappingType.AWS_AMI.name());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getInstanceType_valid() {
    assertThat(awsLambdaInstanceHandler.getInstanceType(InfrastructureMappingType.AWS_AWS_LAMBDA.name()))
        .isEqualTo(ServerlessInstanceType.AWS_LAMBDA);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getInstanceType_invalid_infra() {
    awsLambdaInstanceHandler.getInstanceType(InfrastructureMappingType.AWS_AMI.name());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void generateDeploymentKey() {
    DeploymentKey deploymentKey = awsLambdaInstanceHandler.generateDeploymentKey(
        AwsLambdaDeploymentInfo.builder().functionName("name").version("version").build());
    assertThat(deploymentKey).isInstanceOf(AwsLambdaDeploymentKey.class);
    AwsLambdaDeploymentKey key = (AwsLambdaDeploymentKey) deploymentKey;
    assertThat(key.getFunctionName()).isEqualTo("name");
    assertThat(key.getVersion()).isEqualTo("version");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void setDeploymentKey() {
    DeploymentSummary deploymentSummary = DeploymentSummary.builder().build();
    AwsLambdaDeploymentKey deploymentKey = AwsLambdaDeploymentKey.builder().build();
    awsLambdaInstanceHandler.setDeploymentKey(deploymentSummary, deploymentKey);
    assertThat(deploymentSummary.getAwsLambdaDeploymentKey()).isEqualTo(deploymentKey);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void setDeploymentKey_error() {
    DeploymentSummary deploymentSummary = DeploymentSummary.builder().build();
    AwsAmiDeploymentKey deploymentKey = AwsAmiDeploymentKey.builder().build();
    awsLambdaInstanceHandler.setDeploymentKey(deploymentSummary, deploymentKey);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void fetchInvocationCountFromCloudWatch() {
    AwsCloudWatchHelperServiceDelegate mock = mock(AwsCloudWatchHelperServiceDelegate.class);
    doReturn(mock).when(delegateProxyFactory).get(any(Class.class), any(SyncTaskContext.class));

    awsLambdaInstanceHandler.fetchInvocationCountFromCloudWatch(
        "f1", new Date(), new Date(), "appid", "region", getAwsConfig(), getEncryptionDetails());
    verify(mock, times(1)).getMetricStatistics(any(AwsCloudWatchStatisticsRequest.class));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void secondsBetweenDates() {
    Instant to = Instant.now();
    Instant from = to.minusSeconds(10);
    assertThat(awsLambdaInstanceHandler.secondsBetweenDates(Date.from(from), Date.from(to))).isEqualTo(10);
    assertThat(awsLambdaInstanceHandler.secondsBetweenDates(Date.from(from), Date.from(from))).isEqualTo(0);
    assertThat(awsLambdaInstanceHandler.secondsBetweenDates(Date.from(to.plusSeconds(11)), Date.from(to)))
        .isEqualTo(-11);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void nearestMultipleOf60() {
    assertThat(awsLambdaInstanceHandler.smallestMultipleOf60GreaterEqualThan(100)).isEqualTo(120);
    assertThat(awsLambdaInstanceHandler.smallestMultipleOf60GreaterEqualThan(120)).isEqualTo(120);
    assertThat(awsLambdaInstanceHandler.smallestMultipleOf60GreaterEqualThan(1)).isEqualTo(60);
    assertThat(awsLambdaInstanceHandler.smallestMultipleOf60GreaterEqualThan(0)).isEqualTo(60);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getStartDate() {
    Instant now = Instant.now();
    assertThat(awsLambdaInstanceHandler.getStartDate(Date.from(now), null, InvocationCountKey.LAST_30_DAYS))
        .isBeforeOrEqualTo(Date.from(now.plus(-30, ChronoUnit.DAYS)));
    assertThat(awsLambdaInstanceHandler.getStartDate(null, Date.from(now), InvocationCountKey.SINCE_LAST_DEPLOYED))
        .isEqualTo(Date.from(now));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getStepExecutionSummary() {
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    CommandStepExecutionSummary commandStepExecutionSummary1 = new CommandStepExecutionSummary();
    CommandStepExecutionSummary commandStepExecutionSummary2 = new CommandStepExecutionSummary();
    phaseStepExecutionSummary.setStepExecutionSummaryList(
        Arrays.asList(commandStepExecutionSummary1, commandStepExecutionSummary2));
    assertThat(awsLambdaInstanceHandler.getStepExecutionSummary(phaseStepExecutionSummary).get())
        .isEqualTo(commandStepExecutionSummary1);
    assertThat(awsLambdaInstanceHandler.getStepExecutionSummary(new PhaseStepExecutionSummary()))
        .isEqualTo(Optional.empty());

    PhaseStepExecutionSummary phaseStepExecutionSummary1 = new PhaseStepExecutionSummary();
    phaseStepExecutionSummary1.setStepExecutionSummaryList(
        Collections.singletonList(AmiStepExecutionSummary.builder().build()));
    assertThat(awsLambdaInstanceHandler.getStepExecutionSummary(phaseStepExecutionSummary1))
        .isEqualTo(Optional.empty());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void prepareInvocationCountList() {
    InvocationCount invocationCount = InvocationCount.builder().build();
    doReturn(Optional.of(invocationCount))
        .when(awsLambdaInstanceHandler)
        .getInvocationCountForKey(anyString(), any(Date.class), any(InvocationCountKey.class), anyString(), anyString(),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    List<InvocationCount> invocationCounts = awsLambdaInstanceHandler.prepareInvocationCountList("helloworkld",
        new Date(), Arrays.asList(InvocationCountKey.LAST_30_DAYS, InvocationCountKey.SINCE_LAST_DEPLOYED), "appid",
        "region", AwsConfig.builder().build(), Collections.emptyList());
    assertThat(invocationCounts).isEqualTo(Arrays.asList(invocationCount, invocationCount));
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getInvocationCountForKey() {
    Date starDate = new Date();
    doReturn(starDate)
        .when(awsLambdaInstanceHandler)
        .getStartDate(any(Date.class), any(Date.class), any(InvocationCountKey.class));

    Datapoint datapoint1 = new Datapoint().withSum(100d);
    Datapoint datapoint2 = new Datapoint().withSum(50d);
    AwsCloudWatchStatisticsResponse cloudWatchStatisticsResponse =
        AwsCloudWatchStatisticsResponse.builder().datapoints(Arrays.asList(datapoint1, datapoint2)).build();

    doReturn(cloudWatchStatisticsResponse)
        .when(awsLambdaInstanceHandler)
        .fetchInvocationCountFromCloudWatch(anyString(), any(Date.class), any(Date.class), anyString(), anyString(),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
    {
      Optional<InvocationCount> invocationCountForKey =
          awsLambdaInstanceHandler.getInvocationCountForKey("hello", new Date(), InvocationCountKey.LAST_30_DAYS,
              "appid", "region", AwsConfig.builder().build(), Collections.emptyList());
      assertThat(invocationCountForKey.get().getCount()).isEqualTo(150);
      assertThat(invocationCountForKey.get().getFrom()).isEqualTo(starDate.toInstant());
      assertThat(invocationCountForKey.get().getTo()).isAfterOrEqualTo(starDate.toInstant());
      assertThat(invocationCountForKey.get().getKey()).isEqualTo(InvocationCountKey.LAST_30_DAYS);
    }

    {
      doReturn(AwsCloudWatchStatisticsResponse.builder().build())
          .when(awsLambdaInstanceHandler)
          .fetchInvocationCountFromCloudWatch(anyString(), any(Date.class), any(Date.class), anyString(), anyString(),
              any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

      Optional<InvocationCount> invocationCountForKey =
          awsLambdaInstanceHandler.getInvocationCountForKey("hello", new Date(), InvocationCountKey.LAST_30_DAYS,
              "appid", "region", AwsConfig.builder().build(), Collections.emptyList());
      assertThat(invocationCountForKey.isPresent()).isEqualTo(false);
    }
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getInvocationCountForKey_error() {
    doThrow(new RuntimeException("error"))
        .when(awsLambdaInstanceHandler)
        .getStartDate(any(Date.class), any(Date.class), any(InvocationCountKey.class));

    awsLambdaInstanceHandler.getInvocationCountForKey("hello", new Date(), InvocationCountKey.LAST_30_DAYS, "appid",
        "region", AwsConfig.builder().build(), Collections.emptyList());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testSyncInstances() {
    doNothing()
        .when(awsLambdaInstanceHandler)
        .syncInstancesInternal(anyString(), anyString(), anyListOf(DeploymentSummary.class));
    awsLambdaInstanceHandler.syncInstances("appid", "innfraid");

    verify(awsLambdaInstanceHandler, times(1))
        .syncInstancesInternal(anyString(), anyString(), anyListOf(DeploymentSummary.class));

    ArgumentCaptor<List> argumentcaptor = ArgumentCaptor.forClass(List.class);
    verify(awsLambdaInstanceHandler, times(1))
        .syncInstancesInternal(anyString(), anyString(), argumentcaptor.capture());
    assertThat(argumentcaptor.getValue().isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getActiveServerlessInstances() {
    doReturn(aPageResponse()
                 .withResponse(Arrays.asList(ServerlessInstance.builder().uuid("uid1").build(),
                     ServerlessInstance.builder().uuid("uid2").build()))
                 .build())
        .when(serverlessInstanceService)
        .list(any(PageRequest.class));
    Collection<ServerlessInstance> activeServerlessInstances =
        awsLambdaInstanceHandler.getActiveServerlessInstances("appid", "infraid");
    assertThat(activeServerlessInstances.stream().map(ServerlessInstance::getUuid).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList("uid1", "uid2"));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getInfraMapping() {
    AwsLambdaInfraStructureMapping lambdaInfraStructureMappingOriginal =
        AwsLambdaInfraStructureMapping.builder().build();
    doReturn(lambdaInfraStructureMappingOriginal).when(infraMappingService).get(anyString(), anyString());
    assertThat(awsLambdaInstanceHandler.getInfraMapping("appid", "infraid"))
        .isEqualTo(lambdaInfraStructureMappingOriginal);
  }
  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getInfraMapping_invalid() {
    doReturn(new AwsAmiInfrastructureMapping()).when(infraMappingService).get(anyString(), anyString());
    awsLambdaInstanceHandler.getInfraMapping("appid", "infraid");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void cloudProviderSetting() {
    SettingAttribute settingAttribute = new SettingAttribute();
    doReturn(settingAttribute).when(settingsService).get(anyString());

    assertThat(awsLambdaInstanceHandler.cloudProviderSetting(
                   AwsLambdaInfraStructureMapping.builder().computeProviderSettingId("computeproviderid").build()))
        .isEqualTo(settingAttribute);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void cloudProviderSetting_invalid() {
    doReturn(null).when(settingsService).get(anyString());
    awsLambdaInstanceHandler.cloudProviderSetting(
        AwsLambdaInfraStructureMapping.builder().computeProviderSettingId("computeproviderid").build());
  }

  @Test()
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getEncryptedDataDetails() {
    doReturn(Collections.emptyList())
        .when(secretManager)
        .getEncryptionDetails(any(EncryptableSetting.class), anyString(), anyString());
    assertThat(awsLambdaInstanceHandler.getEncryptedDataDetails(new SettingAttribute()))
        .isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void syncInstancesInternal_instance_sync_flow() {
    setInfraMapping();
    setCloudProvider();
    setEncryptedDataDetails();

    setupAwsLambdaInstanceHandler();

    awsLambdaInstanceHandler.syncInstancesInternal("appid", "inframappinfid", Collections.emptyList());
    verify(awsLambdaInstanceHandler, times(2))
        .syncInDBInstance(any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class),
            anyListOf(EncryptedDataDetail.class), any(ServerlessInstance.class));

    verify(awsLambdaInstanceHandler, times(0))
        .handleNewDeploymentInternal(anyListOf(DeploymentSummary.class), anyListOf(ServerlessInstance.class),
            any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
  }

  private void setupAwsLambdaInstanceHandler() {
    doReturn(Arrays.asList(
                 ServerlessInstance.builder().uuid("uid1").build(), ServerlessInstance.builder().uuid("uid2").build()))
        .when(awsLambdaInstanceHandler)
        .getActiveServerlessInstances(anyString(), anyString());

    doNothing()
        .when(awsLambdaInstanceHandler)
        .syncInDBInstance(any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class),
            anyListOf(EncryptedDataDetail.class), any(ServerlessInstance.class));
  }

  @Test()
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void syncInstancesInternal_new_deployment() {
    setInfraMapping();
    setCloudProvider();
    setEncryptedDataDetails();

    setupAwsLambdaInstanceHandler();

    doNothing()
        .when(awsLambdaInstanceHandler)
        .handleNewDeploymentInternal(anyListOf(DeploymentSummary.class), anyListOf(ServerlessInstance.class),
            any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    List<DeploymentSummary> newDeploymentSummaries =
        Arrays.asList(DeploymentSummary.builder().build(), DeploymentSummary.builder().build());
    awsLambdaInstanceHandler.syncInstancesInternal("appid", "inframappinfid", newDeploymentSummaries);

    verify(awsLambdaInstanceHandler, times(0))
        .syncInDBInstance(any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class),
            anyListOf(EncryptedDataDetail.class), any(ServerlessInstance.class));

    verify(awsLambdaInstanceHandler, times(1))
        .handleNewDeploymentInternal(eq(newDeploymentSummaries), anyListOf(ServerlessInstance.class),
            any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
  }

  private SettingAttribute setCloudProvider() {
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setValue(AwsConfig.builder().build());
    doReturn(settingAttribute).when(settingsService).get(anyString());
    return settingAttribute;
  }

  private AwsLambdaInfraStructureMapping setInfraMapping() {
    AwsLambdaInfraStructureMapping inframapping = AwsLambdaInfraStructureMapping.builder().build();
    doReturn(inframapping).when(infraMappingService).get(anyString(), anyString());
    return inframapping;
  }

  private List<EncryptedDataDetail> setEncryptedDataDetails() {
    doReturn(Collections.emptyList())
        .when(secretManager)
        .getEncryptionDetails(any(EncryptableSetting.class), anyString(), anyString());
    return Collections.emptyList();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void deleteInstances() {
    doReturn(true).when(serverlessInstanceService).delete(anyListOf(String.class));
    List<ServerlessInstance> serverlessInstances = getServerlessInstances();
    awsLambdaInstanceHandler.deleteInstances(serverlessInstances);

    ArgumentCaptor<List> argumentcaptor = ArgumentCaptor.forClass(List.class);
    verify(serverlessInstanceService, times(1)).delete(argumentcaptor.capture());
    assertThat(argumentcaptor.getValue())
        .isEqualTo(serverlessInstances.stream().map(ServerlessInstance::getUuid).collect(Collectors.toList()));
  }

  private List<ServerlessInstance> getServerlessInstances() {
    return Arrays.asList(
        ServerlessInstance.builder().uuid("uid1").build(), ServerlessInstance.builder().uuid("uid2").build());
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void handleNewDeploymentInternal() {
    doNothing()
        .when(awsLambdaInstanceHandler)
        .handleNewDeploymentSummary(any(DeploymentSummary.class), any(AwsLambdaInfraStructureMapping.class),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));
    doNothing().when(awsLambdaInstanceHandler).deleteInstances(anyListOf(ServerlessInstance.class));
    awsLambdaInstanceHandler.handleNewDeploymentInternal(
        getDeploymentSummarList(), getServerlessInstances(), getInframapping(), getAwsConfig(), getEncryptionDetails());
    verify(awsLambdaInstanceHandler, times(1)).deleteInstances(getServerlessInstances());
    verify(awsLambdaInstanceHandler, times(2))
        .handleNewDeploymentSummary(
            any(DeploymentSummary.class), eq(getInframapping()), eq(getAwsConfig()), eq(getEncryptionDetails()));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void handleNewDeploymentSummary() {
    doReturn(AwsLambdaInstanceInfo.builder().build())
        .when(awsLambdaInstanceHandler)
        .getLambdaInstanceInfo(anyString(), anyString(), any(Date.class), any(AwsLambdaInfraStructureMapping.class),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    doReturn(getServerlessInstance())
        .when(awsLambdaInstanceHandler)
        .buildInstanceForNewDeployment(
            any(AwsLambdaInfraStructureMapping.class), any(DeploymentSummary.class), any(AwsLambdaInstanceInfo.class));

    doReturn(getServerlessInstance()).when(serverlessInstanceService).save(any(ServerlessInstance.class));

    awsLambdaInstanceHandler.handleNewDeploymentSummary(
        getDeploymentSummary(), getInframapping(), getAwsConfig(), getEncryptionDetails());
    verify(serverlessInstanceService, times(1)).save(getServerlessInstance());
  }

  @Test(expected = None.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void handleNewDeploymentSummary_error() {
    doThrow(new RuntimeException("error"))
        .when(awsLambdaInstanceHandler)
        .getLambdaInstanceInfo(anyString(), anyString(), any(Date.class), any(AwsLambdaInfraStructureMapping.class),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    awsLambdaInstanceHandler.handleNewDeploymentSummary(
        getDeploymentSummary(), getInframapping(), getAwsConfig(), getEncryptionDetails());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void buildInstanceForNewDeployment() {
    doReturn(ServerlessInstance.builder())
        .when(awsLambdaInstanceHandler)
        .buildServerlessInstance(getInframapping(), getDeploymentSummary());

    doReturn(ServerlessInstance.builder())
        .when(awsLambdaInstanceHandler)
        .populateWithArtifactDetails(anyString(), any(ServerlessInstanceBuilder.class));
    AwsLambdaInstanceInfo awsLambdaInstanceInfo = AwsLambdaInstanceInfo.builder().build();
    ServerlessInstance serverlessInstance = awsLambdaInstanceHandler.buildInstanceForNewDeployment(
        getInframapping(), getDeploymentSummary(), awsLambdaInstanceInfo);
    assertThat(serverlessInstance.getInstanceInfo()).isEqualTo(awsLambdaInstanceInfo);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void populateWithArtifactDetails() {
    Artifact artifact = new Artifact();
    artifact.setUuid("artifactid");
    doReturn(artifact).when(artifactService).get(anyString());
    ServerlessInstanceBuilder builder = ServerlessInstance.builder();
    ServerlessInstanceBuilder serverlessInstanceBuilder =
        awsLambdaInstanceHandler.populateWithArtifactDetails("artifactid", builder);
    assertThat(serverlessInstanceBuilder).isEqualTo(builder);
    assertThat(serverlessInstanceBuilder.build().getLastArtifactId()).isEqualTo("artifactid");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void syncInDBInstance_with_updates() {
    AwsLambdaInstanceInfo awsLambdaInstanceInfo = setLambdaInstanceInfo();
    doReturn(true)
        .when(awsLambdaInstanceHandler)
        .somethingUpdated(any(ServerlessInstanceInfo.class), any(ServerlessInstanceInfo.class));

    doReturn(getServerlessInstance())
        .when(awsLambdaInstanceHandler)
        .handleNewUpdatesToInstance(any(ServerlessInstance.class), any(AwsLambdaInstanceInfo.class));
    awsLambdaInstanceHandler.syncInDBInstance(
        getInframapping(), getAwsConfig(), getEncryptionDetails(), getServerlessInstance());
    verify(awsLambdaInstanceHandler, times(1))
        .handleNewUpdatesToInstance(getServerlessInstance(), awsLambdaInstanceInfo);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void syncInDBInstance_with_noupdate() {
    AwsLambdaInstanceInfo awsLambdaInstanceInfo = setLambdaInstanceInfo();
    doReturn(false)
        .when(awsLambdaInstanceHandler)
        .somethingUpdated(any(ServerlessInstanceInfo.class), any(ServerlessInstanceInfo.class));

    doReturn(getServerlessInstance())
        .when(awsLambdaInstanceHandler)
        .handleNewUpdatesToInstance(any(ServerlessInstance.class), any(AwsLambdaInstanceInfo.class));
    awsLambdaInstanceHandler.syncInDBInstance(
        getInframapping(), getAwsConfig(), getEncryptionDetails(), getServerlessInstance());
    verify(awsLambdaInstanceHandler, times(0))
        .handleNewUpdatesToInstance(getServerlessInstance(), awsLambdaInstanceInfo);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void syncInDBInstance_function_deleted() {
    doReturn(null)
        .when(awsLambdaInstanceHandler)
        .getLambdaInstanceInfo(anyString(), anyString(), any(Date.class), any(AwsLambdaInfraStructureMapping.class),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    doReturn(false)
        .when(awsLambdaInstanceHandler)
        .somethingUpdated(any(ServerlessInstanceInfo.class), any(ServerlessInstanceInfo.class));

    doReturn(getServerlessInstance())
        .when(awsLambdaInstanceHandler)
        .handleNewUpdatesToInstance(any(ServerlessInstance.class), any(AwsLambdaInstanceInfo.class));

    doNothing().when(awsLambdaInstanceHandler).handleFunctionNotExist(any(ServerlessInstance.class));

    awsLambdaInstanceHandler.syncInDBInstance(
        getInframapping(), getAwsConfig(), getEncryptionDetails(), getServerlessInstance());
    verify(awsLambdaInstanceHandler, times(1)).handleFunctionNotExist(getServerlessInstance());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_buildServerlessInstance() {
    setAppService();
    setEnvironmentService();
    setService();
    ServerlessInstanceBuilder serverlessInstanceBuilder =
        awsLambdaInstanceHandler.buildServerlessInstance(getInframapping(), getDeploymentSummary());
    ServerlessInstance instance = serverlessInstanceBuilder.build();
    assertThat(instance.getUuid()).isNotNull();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getDeploymentInfo() {
    CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
    commandStepExecutionSummary.setArtifactId("artifactid");
    commandStepExecutionSummary.setLambdaFunctionMetaList(Arrays.asList(FunctionMeta.builder().build()));
    doReturn(Optional.of(commandStepExecutionSummary))
        .when(awsLambdaInstanceHandler)
        .getStepExecutionSummary(any(PhaseStepExecutionSummary.class));

    Optional<List<DeploymentInfo>> deploymentInfo = awsLambdaInstanceHandler.getDeploymentInfo(null,
        PhaseStepExecutionDataBuilder.aPhaseStepExecutionData()
            .withPhaseStepExecutionSummary(new PhaseStepExecutionSummary())
            .build(),
        null, getInframapping(), "stateexecutionid", Artifact.Builder.anArtifact().withUuid("artifactid").build());

    assertThat(deploymentInfo.isPresent()).isTrue();
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_createLambdaInstanceInfo() {
    AwsLambdaDetails awsLambdaDetails = AwsLambdaDetails.builder().tags(ImmutableMap.of("key", "value")).build();
    AwsLambdaInstanceInfo lambdaInstanceInfo =
        awsLambdaInstanceHandler.createLambdaInstanceInfo(awsLambdaDetails, Collections.emptyList());
    assertThat(lambdaInstanceInfo.getTags()).contains(Tag.builder().key("key").value("value").build());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getLambdaInstanceInfo() {
    AwsLambdaDetails awsLambdaDetails =
        AwsLambdaDetails.builder().functionName("f1").aliases(Collections.singletonList("alias")).build();
    doReturn(awsLambdaDetails)
        .when(awsLambdaInstanceHandler)
        .getFunctionDetails(any(AwsLambdaInfraStructureMapping.class), any(AwsConfig.class),
            anyListOf(EncryptedDataDetail.class), anyString(), anyString());

    doReturn(Collections.emptyList())
        .when(awsLambdaInstanceHandler)
        .prepareInvocationCountList(anyString(), any(Date.class), anyListOf(InvocationCountKey.class), anyString(),
            anyString(), any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    AwsLambdaInstanceInfo instanceInfo = awsLambdaInstanceHandler.getLambdaInstanceInfo(
        "f1", "1", new Date(), getInframapping(), getAwsConfig(), getEncryptionDetails());

    assertThat(instanceInfo.getFunctionName()).isEqualTo("f1");
    assertThat(instanceInfo.getAliases()).contains("alias");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void handle_handleNewDeployment() {
    OnDemandRollbackInfo onDemandRollbackInfo = OnDemandRollbackInfo.builder().onDemandRollback(false).build();
    doNothing()
        .when(awsLambdaInstanceHandler)
        .syncInstancesInternal(anyString(), anyString(), anyListOf(DeploymentSummary.class));
    awsLambdaInstanceHandler.handleNewDeployment(getDeploymentSummarList(), false, onDemandRollbackInfo);
    verify(awsLambdaInstanceHandler, times(1))
        .syncInstancesInternal(anyString(), anyString(), eq(getDeploymentSummarList()));
  }
  private List<DeploymentSummary> getDeploymentSummarList() {
    return Arrays.asList(DeploymentSummary.builder().build(), DeploymentSummary.builder().build());
  }

  private AwsLambdaInfraStructureMapping getInframapping() {
    return AwsLambdaInfraStructureMapping.builder()
        .infraMappingType(InfrastructureMappingType.AWS_AWS_LAMBDA.name())
        .build();
  }

  private AwsConfig getAwsConfig() {
    return AwsConfig.builder().build();
  }
  private List<EncryptedDataDetail> getEncryptionDetails() {
    return Collections.emptyList();
  }
  private ServerlessInstance getServerlessInstance() {
    return ServerlessInstance.builder()
        .uuid("uuid1")
        .lambdaInstanceKey(AwsLambdaInstanceKey.builder().functionName("fn1").functionVersion("1").build())
        .build();
  }

  private DeploymentSummary getDeploymentSummary() {
    return DeploymentSummary.builder().deploymentInfo(AwsLambdaDeploymentInfo.builder().build()).build();
  }

  private AwsLambdaInstanceInfo setLambdaInstanceInfo() {
    AwsLambdaInstanceInfo instanceInfo = AwsLambdaInstanceInfo.builder().build();
    doReturn(instanceInfo)
        .when(awsLambdaInstanceHandler)
        .getLambdaInstanceInfo(anyString(), anyString(), any(Date.class), any(AwsLambdaInfraStructureMapping.class),
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class));

    return instanceInfo;
  }

  private Application setAppService() {
    Application application = Builder.anApplication().build();
    doReturn(application).when(appService).get(anyString());
    return application;
  }

  private Environment setEnvironmentService() {
    Environment environment = Environment.Builder.anEnvironment().build();
    doReturn(environment).when(environmentService).get(anyString(), anyString(), anyBoolean());
    return environment;
  }

  private Service setService() {
    Service service = Service.builder().build();
    doReturn(service).when(serviceResourceService).getWithDetails(anyString(), anyString());
    return service;
  }
}