/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.sm.states.spotinst.SpotInstDeployState.SPOTINST_DEPLOY_COMMAND;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;

import software.wings.api.ExecutionDataValue;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class SpotinstTrafficShiftAlbDeployExecutionDataTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testData() {
    SpotinstTrafficShiftAlbDeployExecutionData data =
        SpotinstTrafficShiftAlbDeployExecutionData.builder()
            .activityId(ACTIVITY_ID)
            .serviceId(SERVICE_ID)
            .envId(ENV_ID)
            .appId(APP_ID)
            .infraMappingId(INFRA_MAPPING_ID)
            .commandName(SPOTINST_DEPLOY_COMMAND)
            .newElastigroupOriginalConfig(
                ElastiGroup.builder()
                    .id("newId")
                    .name("foo__STAGE__Harness")
                    .capacity(ElastiGroupCapacity.builder().maximum(1).minimum(0).target(1).build())
                    .build())
            .oldElastigroupOriginalConfig(
                ElastiGroup.builder()
                    .id("oldId")
                    .name("foo")
                    .capacity(ElastiGroupCapacity.builder().minimum(0).target(1).maximum(1).build())
                    .build())
            .build();
    SpotinstDeployExecutionSummary stepExecutionSummary = data.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getOldElastigroupId()).isEqualTo("oldId");
    assertThat(stepExecutionSummary.getNewElastigroupId()).isEqualTo("newId");
    Map<String, ExecutionDataValue> executionSummary = data.getExecutionSummary();
    assertThat(executionSummary).isNotNull();
    assertThat(executionSummary.containsKey("activityId")).isTrue();
    assertThat(executionSummary.containsKey("New Elastigroup Id")).isTrue();
    assertThat(executionSummary.containsKey("New Elastigroup Name")).isTrue();
  }
}
