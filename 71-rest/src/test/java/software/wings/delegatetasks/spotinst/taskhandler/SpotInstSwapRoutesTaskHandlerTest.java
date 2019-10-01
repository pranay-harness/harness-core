package software.wings.delegatetasks.spotinst.taskhandler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
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

import java.util.Optional;

public class SpotInstSwapRoutesTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private SpotInstHelperServiceDelegate mockSpotInstHelperServiceDelegate;
  @Mock private AwsElbHelperServiceDelegate mockAwsElbHelperServiceDelegate;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;

  @Spy @Inject @InjectMocks SpotInstSwapRoutesTaskHandler spotInstSwapRoutesTaskHandler;

  @Test
  @Category(UnitTests.class)
  public void testExecuteDeploy() throws Exception {
    String oldId = "oldId";
    String newId = "newId";
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockCallback).saveExecutionLog(anyString(), any(), any());
    doReturn(mockCallback).when(spotInstSwapRoutesTaskHandler).getLogCallBack(any(), anyString());
    doReturn(Optional.of(ElastiGroup.builder()
                             .id(oldId)
                             .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(1).target(1).build())
                             .build()))
        .when(mockSpotInstHelperServiceDelegate)
        .getElastiGroupById(anyString(), anyString(), anyString());
    SpotInstSwapRoutesTaskParameters parameters =
        SpotInstSwapRoutesTaskParameters.builder()
            .rollback(false)
            .downsizeOldElastiGroup(true)
            .elastiGroupNamePrefix("foo")
            .newElastiGroup(ElastiGroup.builder().id(newId).name("foo__STAGE__Harness").build())
            .oldElastiGroup(ElastiGroup.builder().id(oldId).name("foo").build())
            .build();
    spotInstSwapRoutesTaskHandler.executeTaskInternal(parameters,
        SpotInstConfig.builder().spotInstAccountId("SPOTINST_ACCOUNT_ID").spotInstToken(new char[] {'a', 'b'}).build(),
        AwsConfig.builder().build());
    verify(mockAwsElbHelperServiceDelegate).updateListenersForBGDeployment(any(), anyList(), anyList(), anyString());
    verify(mockSpotInstHelperServiceDelegate).updateElastiGroupCapacity(anyString(), anyString(), anyString(), any());
  }
}