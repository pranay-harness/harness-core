/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class MigrateServiceNowCriteriaInPipelinesTest extends WingsBaseTest {
  private final String SNOW_PARAMS = "serviceNowApprovalParams";
  private final String APPROVAL_PARAMS = "approvalStateParams";
  @InjectMocks @Inject private MigrateServiceNowCriteriaInPipelines migrateSNOW;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void migratePipelineWithMultipleSnowApprovals() {
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().type("APPROVAL").properties(fetchApprovalProperties()).build();
    PipelineStageElement pipelineStageElement1 =
        PipelineStageElement.builder().type("APPROVAL").properties(fetchApprovalProperties()).build();
    PipelineStage stage1 = PipelineStage.builder()
                               .name("STAGE 1")
                               .pipelineStageElements(Collections.singletonList(pipelineStageElement))
                               .build();
    PipelineStage stage2 = PipelineStage.builder()
                               .name("STAGE 2")
                               .pipelineStageElements(Collections.singletonList(pipelineStageElement1))
                               .build();
    Pipeline pipeline =
        Pipeline.builder().name("SNOW_PIPELINE").appId("APP_ID").pipelineStages(Arrays.asList(stage1, stage2)).build();
    migrateSNOW.migrate(pipeline);

    Map<String, Object> approvalStateParams1 =
        (Map<String, Object>) stage1.getPipelineStageElements().get(0).getProperties().get(APPROVAL_PARAMS);
    Map<String, Object> approvalStateParams2 =
        (Map<String, Object>) stage2.getPipelineStageElements().get(0).getProperties().get(APPROVAL_PARAMS);
    assertThat(((Map<String, Object>) approvalStateParams1.get(SNOW_PARAMS)).get("approval"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Closed")), "operator", "AND"));
    assertThat(((Map<String, Object>) approvalStateParams1.get(SNOW_PARAMS)).get("rejection"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Canceled")), "operator", "AND"));
    assertThat(((Map<String, Object>) approvalStateParams2.get(SNOW_PARAMS)).get("approval"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Closed")), "operator", "AND"));
    assertThat(((Map<String, Object>) approvalStateParams2.get(SNOW_PARAMS)).get("rejection"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Canceled")), "operator", "AND"));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void migratePipelineWithSnowAndUserGroupApprovals() {
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().type("APPROVAL").properties(fetchApprovalProperties()).build();
    PipelineStageElement pipelineStageElement1 =
        PipelineStageElement.builder().type("APPROVAL").properties(fetchApprovalProperties()).build();
    PipelineStage stage1 = PipelineStage.builder()
                               .name("STAGE 1")
                               .pipelineStageElements(Collections.singletonList(pipelineStageElement))
                               .build();
    PipelineStage stage2 = PipelineStage.builder()
                               .name("STAGE 2")
                               .pipelineStageElements(Collections.singletonList(
                                   PipelineStageElement.builder()
                                       .type("APPROVAL")
                                       .properties(Collections.singletonMap("approvalStateType", "USER_GROUP"))
                                       .build()))
                               .build();
    Pipeline pipeline =
        Pipeline.builder().name("SNOW_PIPELINE").appId("APP_ID").pipelineStages(Arrays.asList(stage1, stage2)).build();
    migrateSNOW.migrate(pipeline);

    Map<String, Object> approvalStateParams1 =
        (Map<String, Object>) stage1.getPipelineStageElements().get(0).getProperties().get(APPROVAL_PARAMS);
    assertThat(((Map<String, Object>) approvalStateParams1.get(SNOW_PARAMS)).get("approval"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Closed")), "operator", "AND"));
    assertThat(((Map<String, Object>) approvalStateParams1.get(SNOW_PARAMS)).get("rejection"))
        .isEqualTo(ImmutableMap.of(
            "conditions", ImmutableMap.of("state", Collections.singletonList("Canceled")), "operator", "AND"));
  }

  private Map<String, Object> fetchApprovalProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateType", "SERVICENOW");
    Map<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approvalField", "state");
    serviceNowApprovalParams.put("approvalValue", "Closed");
    serviceNowApprovalParams.put("rejectionField", "state");
    serviceNowApprovalParams.put("rejectionValue", "Canceled");
    properties.put(APPROVAL_PARAMS, Collections.singletonMap(SNOW_PARAMS, serviceNowApprovalParams));
    return properties;
  }
}
