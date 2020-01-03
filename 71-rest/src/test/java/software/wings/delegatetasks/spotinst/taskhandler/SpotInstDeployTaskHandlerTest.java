package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskResponse;
import io.harness.rule.Owner;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

public class SpotInstDeployTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private SpotInstHelperServiceDelegate mockSpotInstHelperServiceDelegate;
  @Mock private AwsElbHelperServiceDelegate mockAwsElbHelperServiceDelegate;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;

  @Spy @Inject @InjectMocks SpotInstDeployTaskHandler deployTaskHandler;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testScaleElastigroup() throws Exception {
    doNothing()
        .when(deployTaskHandler)
        .updateElastiGroupAndWait(anyString(), anyString(), any(), anyInt(), any(), anyString(), anyString());
    ElastiGroup group = ElastiGroup.builder().build();
    deployTaskHandler.scaleElastigroup(
        group, "TOKEN", "ACCOUNT_ID", 5, SpotInstDeployTaskParameters.builder().build(), "SCALE", "WAIT");
    verify(deployTaskHandler)
        .updateElastiGroupAndWait(anyString(), anyString(), any(), anyInt(), any(), anyString(), anyString());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testScaleElastigroupNull() throws Exception {
    doNothing().when(deployTaskHandler).createAndFinishEmptyExecutionLog(any(), anyString(), anyString());
    deployTaskHandler.scaleElastigroup(
        null, "TOKEN", "ACCOUNT_ID", 5, SpotInstDeployTaskParameters.builder().build(), "SCALE", "WAIT");
    verify(deployTaskHandler, times(2)).createAndFinishEmptyExecutionLog(any(), anyString(), anyString());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteTaskInternal() throws Exception {
    ElastiGroup newElastigroup = ElastiGroup.builder()
                                     .id("newId")
                                     .name("foo__2")
                                     .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                                     .build();
    ElastiGroup oldElastigroup = ElastiGroup.builder()
                                     .id("oldId")
                                     .name("foo__1")
                                     .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                                     .build();
    doNothing()
        .when(deployTaskHandler)
        .scaleElastigroup(any(), anyString(), anyString(), anyInt(), any(), anyString(), anyString());
    doReturn(singletonList(new Instance().withInstanceId("id-new")))
        .doReturn(singletonList(new Instance().withInstanceId("id-old")))
        .when(deployTaskHandler)
        .getAllEc2InstancesOfElastiGroup(any(), anyString(), anyString(), anyString(), anyString());
    SpotInstDeployTaskParameters parameters = SpotInstDeployTaskParameters.builder()
                                                  .newElastiGroupWithUpdatedCapacity(newElastigroup)
                                                  .oldElastiGroupWithUpdatedCapacity(oldElastigroup)
                                                  .blueGreen(false)
                                                  .rollback(false)
                                                  .resizeNewFirst(true)
                                                  .build();
    SpotInstTaskExecutionResponse response = deployTaskHandler.executeTaskInternal(parameters,
        SpotInstConfig.builder().spotInstAccountId("SPOTINST_ACCOUNT_ID").spotInstToken(new char[] {'a', 'b'}).build(),
        AwsConfig.builder().build());
    assertThat(response).isNotNull();
    SpotInstTaskResponse spotInstTaskResponse = response.getSpotInstTaskResponse();
    assertThat(spotInstTaskResponse).isNotNull();
    assertThat(spotInstTaskResponse instanceof SpotInstDeployTaskResponse).isTrue();
    SpotInstDeployTaskResponse deployTaskResponse = (SpotInstDeployTaskResponse) spotInstTaskResponse;
    assertThat(deployTaskResponse.getEc2InstancesAdded().size()).isEqualTo(1);
    assertThat(deployTaskResponse.getEc2InstancesExisting().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalResizeOldFirst() throws Exception {
    ElastiGroup newElastigroup = ElastiGroup.builder()
                                     .id("newId")
                                     .name("foo__2")
                                     .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                                     .build();
    ElastiGroup oldElastigroup = ElastiGroup.builder()
                                     .id("oldId")
                                     .name("foo__1")
                                     .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                                     .build();
    doNothing()
        .when(deployTaskHandler)
        .scaleElastigroup(any(), anyString(), anyString(), anyInt(), any(), anyString(), anyString());
    doReturn(singletonList(new Instance().withInstanceId("id-new")))
        .doReturn(singletonList(new Instance().withInstanceId("id-old")))
        .when(deployTaskHandler)
        .getAllEc2InstancesOfElastiGroup(any(), anyString(), anyString(), anyString(), anyString());
    SpotInstDeployTaskParameters parameters = SpotInstDeployTaskParameters.builder()
                                                  .newElastiGroupWithUpdatedCapacity(newElastigroup)
                                                  .oldElastiGroupWithUpdatedCapacity(oldElastigroup)
                                                  .blueGreen(false)
                                                  .rollback(false)
                                                  .resizeNewFirst(false)
                                                  .build();
    SpotInstTaskExecutionResponse response = deployTaskHandler.executeTaskInternal(parameters,
        SpotInstConfig.builder().spotInstAccountId("SPOTINST_ACCOUNT_ID").spotInstToken(new char[] {'a', 'b'}).build(),
        AwsConfig.builder().build());
    assertThat(response).isNotNull();
    SpotInstTaskResponse spotInstTaskResponse = response.getSpotInstTaskResponse();
    assertThat(spotInstTaskResponse).isNotNull();
    assertThat(spotInstTaskResponse instanceof SpotInstDeployTaskResponse).isTrue();
    SpotInstDeployTaskResponse deployTaskResponse = (SpotInstDeployTaskResponse) spotInstTaskResponse;
    assertThat(deployTaskResponse.getEc2InstancesAdded().size()).isEqualTo(1);
    assertThat(deployTaskResponse.getEc2InstancesExisting().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalRollback() throws Exception {
    ElastiGroup newElastigroup = ElastiGroup.builder()
                                     .id("newId")
                                     .name("foo__2")
                                     .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                                     .build();
    ElastiGroup oldElastigroup = ElastiGroup.builder()
                                     .id("oldId")
                                     .name("foo__1")
                                     .capacity(ElastiGroupCapacity.builder().minimum(1).maximum(1).target(1).build())
                                     .build();
    doNothing()
        .when(deployTaskHandler)
        .scaleElastigroup(any(), anyString(), anyString(), anyInt(), any(), anyString(), anyString());
    doReturn(singletonList(new Instance().withInstanceId("id-old")))
        .when(deployTaskHandler)
        .getAllEc2InstancesOfElastiGroup(any(), anyString(), anyString(), anyString(), anyString());
    SpotInstDeployTaskParameters parameters = SpotInstDeployTaskParameters.builder()
                                                  .newElastiGroupWithUpdatedCapacity(newElastigroup)
                                                  .oldElastiGroupWithUpdatedCapacity(oldElastigroup)
                                                  .blueGreen(false)
                                                  .rollback(true)
                                                  .build();
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockCallback).saveExecutionLog(anyString(), any(), any());
    doReturn(mockCallback).when(deployTaskHandler).getLogCallBack(any(), anyString());
    SpotInstTaskExecutionResponse response = deployTaskHandler.executeTaskInternal(parameters,
        SpotInstConfig.builder().spotInstAccountId("SPOTINST_ACCOUNT_ID").spotInstToken(new char[] {'a', 'b'}).build(),
        AwsConfig.builder().build());
    assertThat(response).isNotNull();
    SpotInstTaskResponse spotInstTaskResponse = response.getSpotInstTaskResponse();
    assertThat(spotInstTaskResponse).isNotNull();
    assertThat(spotInstTaskResponse instanceof SpotInstDeployTaskResponse).isTrue();
    SpotInstDeployTaskResponse deployTaskResponse = (SpotInstDeployTaskResponse) spotInstTaskResponse;
    assertThat(deployTaskResponse.getEc2InstancesAdded().size()).isEqualTo(0);
    assertThat(deployTaskResponse.getEc2InstancesExisting().size()).isEqualTo(1);
    verify(mockSpotInstHelperServiceDelegate).deleteElastiGroup(anyString(), anyString(), anyString());
  }
}