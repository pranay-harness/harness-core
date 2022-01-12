/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class SpotInstListenerUpdateStateExecutionDataTest extends WingsBaseTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testStateData() {
    String oldId = "oldId";
    String oldName = "foo";
    String newId = "newId";
    String newName = "foo__STAGE__Harness";
    SpotInstListenerUpdateStateExecutionData data =
        SpotInstListenerUpdateStateExecutionData.builder()
            .oldElastiGroupId(oldId)
            .oldElastiGroupName(oldName)
            .newElastiGroupId(newId)
            .newElastiGroupName(newName)
            .spotinstCommandRequest(
                SpotInstCommandRequest.builder()
                    .spotInstTaskParameters(SpotInstSwapRoutesTaskParameters.builder().activityId(ACTIVITY_ID).build())
                    .build())
            .isRollback(false)
            .lbDetails(singletonList(LoadBalancerDetailsForBGDeployment.builder()
                                         .loadBalancerName("lb-name")
                                         .prodListenerArn("prod-listener-arn")
                                         .prodListenerPort("8080")
                                         .prodTargetGroupArn("prod-tgt-arn")
                                         .prodTargetGroupName("prod-tgt-name")
                                         .stageListenerArn("stage-listener-arn")
                                         .stageListenerPort("8181")
                                         .stageTargetGroupArn("stage-tgt-arn")
                                         .stageTargetGroupName("stage-tgt-name")
                                         .build()))
            .downsizeOldElastiGroup(true)
            .build();
    Map<String, ExecutionDataValue> executionSummary = data.getExecutionSummary();
    assertThat(executionSummary.size()).isEqualTo(4);
    SpotinstDeployExecutionSummary stepExecutionSummary = data.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getOldElastigroupId()).isEqualTo(oldId);
    assertThat(stepExecutionSummary.getNewElastigroupId()).isEqualTo(newId);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testStateDataForNPE() {
    String oldId = "oldId";
    String oldName = "foo";
    String newId = "newId";
    String newName = "foo__STAGE__Harness";
    SpotInstListenerUpdateStateExecutionData data =
        SpotInstListenerUpdateStateExecutionData.builder()
            .oldElastiGroupId(oldId)
            .oldElastiGroupName(oldName)
            .newElastiGroupId(newId)
            .newElastiGroupName(newName)
            .spotinstCommandRequest(
                SpotInstCommandRequest.builder()
                    .spotInstTaskParameters(SpotInstSwapRoutesTaskParameters.builder().activityId(ACTIVITY_ID).build())
                    .build())
            .isRollback(false)
            .lbDetails(null)
            .downsizeOldElastiGroup(true)
            .build();

    // All assertions are done in above method.
    // here just testing that NPE is not thrown
    assertThat(data.getExecutionSummary()).isNotNull();
    data.setLbDetails(singletonList(LoadBalancerDetailsForBGDeployment.builder()
                                        .loadBalancerName(null)
                                        .prodListenerArn(null)
                                        .prodListenerPort(null)
                                        .prodTargetGroupArn(null)
                                        .prodTargetGroupName(null)
                                        .stageListenerArn(null)
                                        .stageListenerPort(null)
                                        .stageTargetGroupArn(null)
                                        .stageTargetGroupName(null)
                                        .build()));

    // here just testing that NPE is not thrown
    assertThat(data.getExecutionSummary()).isNotNull();
  }
}
