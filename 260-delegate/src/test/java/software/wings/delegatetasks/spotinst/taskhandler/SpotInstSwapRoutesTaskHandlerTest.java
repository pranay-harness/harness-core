/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.rule.Owner;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class SpotInstSwapRoutesTaskHandlerTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private SpotInstHelperServiceDelegate mockSpotInstHelperServiceDelegate;
  @Mock private AwsElbHelperServiceDelegate mockAwsElbHelperServiceDelegate;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private AwsEc2HelperServiceDelegate mockAwsEc2HelperServiceDelegate;

  @Spy @Inject @InjectMocks SpotInstSwapRoutesTaskHandler spotInstSwapRoutesTaskHandler;

  @Test
  @Owner(developers = SATYAM)
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
    verify(mockAwsElbHelperServiceDelegate)
        .updateListenersForSpotInstBGDeployment(any(), anyList(), anyList(), anyString(), any());
    verify(mockSpotInstHelperServiceDelegate).updateElastiGroupCapacity(anyString(), anyString(), anyString(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteRollback() throws Exception {
    ExecutionLogCallback mockCallback = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallback).saveExecutionLog(anyString());
    doNothing().when(mockCallback).saveExecutionLog(anyString(), any(), any());
    doReturn(mockCallback).when(spotInstSwapRoutesTaskHandler).getLogCallBack(any(), anyString());
    doNothing()
        .when(spotInstSwapRoutesTaskHandler)
        .updateElastiGroupAndWait(anyString(), anyString(), any(), anyInt(), any(), anyString(), anyString());
    doReturn(new DescribeListenersResult().withListeners(new Listener().withDefaultActions(
                 new Action().withType("forward").withTargetGroupArn("STAGE_TARGET_GROUP_ARN"))))
        .when(mockAwsElbHelperServiceDelegate)
        .describeListenerResult(any(), anyList(), anyString(), anyString());
    String oldId = "oldId";
    String newId = "newId";
    SpotInstSwapRoutesTaskParameters parameters =
        SpotInstSwapRoutesTaskParameters.builder()
            .rollback(true)
            .elastiGroupNamePrefix("foo")
            .newElastiGroup(ElastiGroup.builder().id(newId).name("foo__STAGE__Harness").build())
            .oldElastiGroup(ElastiGroup.builder().id(oldId).name("foo").build())
            .lBdetailsForBGDeploymentList(Collections.singletonList(LoadBalancerDetailsForBGDeployment.builder()
                                                                        .prodListenerArn("PROD_LITENER_ARN")
                                                                        .stageTargetGroupArn("STAGE_TARGET_GROUP_ARN")
                                                                        .build()))
            .build();
    spotInstSwapRoutesTaskHandler.executeTaskInternal(parameters,
        SpotInstConfig.builder().spotInstAccountId("SPOTINST_ACCOUNT_ID").spotInstToken(new char[] {'a', 'b'}).build(),
        AwsConfig.builder().build());
    verify(mockAwsElbHelperServiceDelegate)
        .updateDefaultListenersForSpotInstBG(any(), anyList(), anyString(), anyString(), anyString());
    verify(spotInstSwapRoutesTaskHandler, times(2))
        .updateElastiGroupAndWait(anyString(), anyString(), any(), anyInt(), any(), anyString(), anyString());
  }
}
