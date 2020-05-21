package io.harness.perpetualtask.instancesync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SPOTINST_CLOUD_PROVIDER;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsAmiInfrastructureMapping.Builder;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpotinstAmiInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  private static final String ELASTIGROUP_ID = "elasti-group-id";
  private static final String TASK_ID = "task-id";

  @Mock PerpetualTaskService perpetualTaskService;
  @Mock InfrastructureMappingService infraMappingService;
  @Mock SettingsService settingsService;
  @Mock SecretManager secretManager;

  @InjectMocks @Inject SpotinstAmiInstanceSyncPerpetualTaskClient client;

  @Before
  public void setUp() {
    SpotInstConfig spotInstConfig = SpotInstConfig.builder().build();
    AwsConfig awsConfig = AwsConfig.builder().build();

    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(Builder.anAwsAmiInfrastructureMapping()
                        .withRegion(US_EAST)
                        .withSpotinstCloudProvider(SPOTINST_CLOUD_PROVIDER)
                        .withComputeProviderName(COMPUTE_PROVIDER_NAME)
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
                        .build());
    when(settingsService.get(SPOTINST_CLOUD_PROVIDER))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(spotInstConfig).build());
    when(settingsService.get(COMPUTE_PROVIDER_SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(awsConfig).build());
    when(secretManager.getEncryptionDetails(spotInstConfig, null, null)).thenReturn(Collections.emptyList());
    when(secretManager.getEncryptionDetails(awsConfig, null, null)).thenReturn(Collections.emptyList());
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void createPerpeturalTask() {
    client.create(ACCOUNT_ID, getPerpetualTaskClientParams());

    verify(perpetualTaskService, times(1))
        .createTask(PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC, ACCOUNT_ID, getPerpetualTaskClientContext(),
            PerpetualTaskSchedule.newBuilder()
                .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                .build(),
            false);
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void resetTask() {
    client.reset(ACCOUNT_ID, TASK_ID);

    verify(perpetualTaskService, times(1)).resetTask(ACCOUNT_ID, TASK_ID);
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void deleteTask() {
    client.delete(ACCOUNT_ID, TASK_ID);

    verify(perpetualTaskService, times(1)).deleteTask(ACCOUNT_ID, TASK_ID);
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void getTaskParams() {
    final SpotinstAmiInstanceSyncPerpetualTaskParams taskParams =
        (SpotinstAmiInstanceSyncPerpetualTaskParams) client.getTaskParams(getPerpetualTaskClientContext());

    assertThat(taskParams.getRegion()).isEqualTo(US_EAST);
    assertThat(taskParams.getElastigroupId()).isEqualTo(ELASTIGROUP_ID);
    assertThat(taskParams.getAwsConfig()).isNotEmpty();
    assertThat(taskParams.getAwsEncryptedData()).isNotEmpty();
    assertThat(taskParams.getSpotinstConfig()).isNotEmpty();
    assertThat(taskParams.getSpotinstEncryptedData()).isNotEmpty();
  }

  @Test
  @Owner(developers = OwnerRule.ABOSII)
  @Category(UnitTests.class)
  public void getValidationTask() {
    DelegateTask delegateTask = client.getValidationTask(getPerpetualTaskClientContext(), ACCOUNT_ID);

    assertThat(delegateTask.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.SPOTINST_COMMAND_TASK.name());
    assertThat(delegateTask.getData().getParameters()).isNotEmpty();
    assertThat(delegateTask.getData().getParameters()[0]).isInstanceOf(SpotInstCommandRequest.class);
    SpotInstCommandRequest commandRequest = (SpotInstCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(commandRequest.getAwsConfig()).isNotNull();
    assertThat(commandRequest.getAwsEncryptionDetails()).isNotNull();
    assertThat(commandRequest.getSpotInstConfig()).isNotNull();
    assertThat(commandRequest.getSpotinstEncryptionDetails()).isNotNull();
    assertThat(commandRequest.getSpotInstTaskParameters())
        .isInstanceOf(SpotInstListElastigroupInstancesParameters.class);
    SpotInstListElastigroupInstancesParameters parameters =
        (SpotInstListElastigroupInstancesParameters) commandRequest.getSpotInstTaskParameters();
    assertThat(parameters.getAwsRegion()).isEqualTo(US_EAST);
    assertThat(parameters.getElastigroupId()).isEqualTo(ELASTIGROUP_ID);
  }

  private SpotinstAmiInstanceSyncPerpetualTaskClientParams getPerpetualTaskClientParams() {
    return SpotinstAmiInstanceSyncPerpetualTaskClientParams.builder()
        .inframappingId(INFRA_MAPPING_ID)
        .appId(APP_ID)
        .elastigroupId(ELASTIGROUP_ID)
        .build();
  }

  private PerpetualTaskClientContext getPerpetualTaskClientContext() {
    return new PerpetualTaskClientContext(ImmutableMap.of(HARNESS_APPLICATION_ID, APP_ID, INFRASTRUCTURE_MAPPING_ID,
        INFRA_MAPPING_ID, SpotinstAmiInstanceSyncPerpetualTaskClient.ELASTIGROUP_ID, ELASTIGROUP_ID));
  }
}