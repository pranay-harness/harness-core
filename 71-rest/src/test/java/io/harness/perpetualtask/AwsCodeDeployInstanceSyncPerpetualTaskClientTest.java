package io.harness.perpetualtask;

import static io.harness.perpetualtask.PerpetualTaskType.AWS_CODE_DEPLOY_INSTANCE_SYNC;
import static io.harness.rule.OwnerRule.ABOSII;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.US_EAST;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import com.amazonaws.services.ec2.model.Filter;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.AwsUtils;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

public class AwsCodeDeployInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private AwsUtils awsUtils;

  @InjectMocks @Inject AwsCodeDeployInstanceSyncPerpetualTaskClient client;

  @Before
  public void setup() {
    CodeDeployInfrastructureMapping infraMapping = aCodeDeployInfrastructureMapping()
                                                       .withAccountId(ACCOUNT_ID)
                                                       .withAppId(APP_ID)
                                                       .withRegion(US_EAST)
                                                       .withComputeProviderSettingId(COMPUTE_PROVIDER_SETTING_ID)
                                                       .withDeploymentType(DeploymentType.AWS_CODEDEPLOY.name())
                                                       .withServiceId(SERVICE_ID)
                                                       .build();
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).build();

    doReturn(infraMapping).when(infraMappingService).get(APP_ID, INFRA_MAPPING_ID);
    doReturn(aSettingAttribute().withValue(awsConfig).build()).when(settingsService).get(COMPUTE_PROVIDER_SETTING_ID);
    doReturn(emptyList()).when(secretManager).getEncryptionDetails(any(EncryptableSetting.class));
    doReturn(DeploymentType.AWS_CODEDEPLOY)
        .when(serviceResourceService)
        .getDeploymentType(infraMapping, null, SERVICE_ID);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreatePerpetualTask() {
    client.create(ACCOUNT_ID,
        AwsCodeDeployInstanceSyncPerpetualTaskClientParams.builder()
            .inframmapingId(INFRA_MAPPING_ID)
            .appId(APP_ID)
            .build());

    verify(perpetualTaskService, times(1))
        .createTask(AWS_CODE_DEPLOY_INSTANCE_SYNC, ACCOUNT_ID, getClientContext(),
            PerpetualTaskSchedule.newBuilder()
                .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                .build(),
            false);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValidationTask() {
    List<Filter> ec2Filters = singletonList(new Filter("instance-state", singletonList("running")));
    doReturn(ec2Filters)
        .when(awsUtils)
        .getAwsFilters(any(AwsInfrastructureMapping.class), eq(DeploymentType.AWS_CODEDEPLOY));
    DelegateTask validationTask = client.getValidationTask(getClientContext(), ACCOUNT_ID);

    verify(infraMappingService, times(1)).get(APP_ID, INFRA_MAPPING_ID);
    verify(settingsService, times(1)).get(COMPUTE_PROVIDER_SETTING_ID);
    verify(secretManager, times(1)).getEncryptionDetails(any(EncryptableSetting.class));

    assertThat(validationTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(validationTask.getAppId()).isEqualTo(GLOBAL_APP_ID);
    assertThat(validationTask.getData().isAsync()).isFalse();
    assertThat(validationTask.getData().getParameters()).isNotEmpty();
    assertThat(validationTask.getData().getParameters()[0]).isInstanceOf(AwsEc2ListInstancesRequest.class);
    AwsEc2ListInstancesRequest awsRequest = (AwsEc2ListInstancesRequest) validationTask.getData().getParameters()[0];
    assertThat(awsRequest.getRegion()).isEqualTo(US_EAST);
    assertThat(awsRequest.getAwsConfig().getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(awsRequest.getEncryptionDetails()).isNotNull();
    assertThat(awsRequest.getFilters()).isEqualTo(ec2Filters);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetTaskParams() {
    AwsCodeDeployInstanceSyncPerpetualTaskParams taskParams =
        (AwsCodeDeployInstanceSyncPerpetualTaskParams) client.getTaskParams(getClientContext());

    assertThat(taskParams.getFilter()).isNotNull();
    assertThat(taskParams.getRegion()).isEqualTo(US_EAST);
    assertThat(taskParams.getAwsConfig()).isNotNull();
    assertThat(taskParams.getEncryptedData()).isNotNull();
  }

  private PerpetualTaskClientContext getClientContext() {
    return new PerpetualTaskClientContext(
        ImmutableMap.of(INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID, HARNESS_APPLICATION_ID, APP_ID));
  }
}