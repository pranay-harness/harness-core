package io.harness.perpetualtask.ecs;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.Map;

public class EcsPerpetualTaskServiceClientTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String taskId = "TASK_ID";
  private static final String REGION = "region";
  private static final String SETTING_ID = "settingId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String CLUSTER_ID = "clusterId";
  private Map<String, String> clientParamsMap = new HashMap<>();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @InjectMocks EcsPerpetualTaskServiceClient ecsPerpetualTaskServiceClient;

  @Before
  public void setUp() {
    when(perpetualTaskService.createTask(eq(PerpetualTaskType.ECS_CLUSTER), eq(accountId),
             isA(PerpetualTaskClientContext.class), isA(PerpetualTaskSchedule.class), eq(false)))
        .thenReturn(taskId);

    when(settingsService.get(SETTING_ID))
        .thenReturn(
            SettingAttribute.Builder.aSettingAttribute()
                .withValue(AwsConfig.builder().accessKey("accessKey").secretKey("secretKey".toCharArray()).build())
                .build());

    when(secretManager.getEncryptionDetails(any(), any(), any())).thenReturn(null);

    clientParamsMap.put(REGION, REGION);
    clientParamsMap.put(SETTING_ID, SETTING_ID);
    clientParamsMap.put(CLUSTER_NAME, CLUSTER_NAME);
    clientParamsMap.put(CLUSTER_ID, CLUSTER_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCreate() {
    EcsPerpetualTaskClientParams params =
        new EcsPerpetualTaskClientParams("region", cloudProviderId, "clusterName", "clusterId");
    ecsPerpetualTaskServiceClient.create(accountId, params);
    verify(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.ECS_CLUSTER), eq(accountId), isA(PerpetualTaskClientContext.class),
            isA(PerpetualTaskSchedule.class), eq(false));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGet() {
    PerpetualTaskClientContext perpetualTaskClientContext = new PerpetualTaskClientContext(clientParamsMap);
    EcsPerpetualTaskParams ecsPerpetualTaskParams =
        ecsPerpetualTaskServiceClient.getTaskParams(perpetualTaskClientContext);
    assertThat(ecsPerpetualTaskParams.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(ecsPerpetualTaskParams.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(ecsPerpetualTaskParams.getRegion()).isEqualTo(REGION);
    assertThat(ecsPerpetualTaskParams.getSettingId()).isEqualTo(SETTING_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetValidationTask() {
    PerpetualTaskClientContext perpetualTaskClientContext = new PerpetualTaskClientContext(clientParamsMap);
    DelegateTask delegateTask = ecsPerpetualTaskServiceClient.getValidationTask(perpetualTaskClientContext, accountId);
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
  }
}
