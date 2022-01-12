/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.expression;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.service.intfc.PipelineService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PipelineExpressionBuilderTest extends WingsBaseTest {
  @Inject @InjectMocks private PipelineExpressionBuilder pipelineExpressionBuilder;
  @Mock private PipelineService pipelineService;

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testShouldGetSupportedExpressionsForTags() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", "BUILD_WORKFLOW_ID");
    PipelineStage pipelineStage1 =
        PipelineStage.builder()
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .workflowVariables(ImmutableMap.of("MyVar", ""))
                                              .name("STAGE1")
                                              .type(ENV_STATE.name())
                                              .properties(properties)
                                              .build()))
            .build();

    Pipeline pipeline2 = Pipeline.builder()
                             .name("pipeline2")
                             .appId(APP_ID)
                             .uuid(PIPELINE_ID)
                             .pipelineStages(asList(pipelineStage1,
                                 PipelineStage.builder()
                                     .pipelineStageElements(asList(PipelineStageElement.builder()
                                                                       .name("STAGE2")
                                                                       .type(ENV_STATE.name())
                                                                       .properties(properties)
                                                                       .build()))
                                     .build()))
                             .build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(ArtifactVariable.builder()
                          .entityId(ENTITY_ID)
                          .name("MyVar")
                          .value(VARIABLE_VALUE)
                          .type(VariableType.TEXT)
                          .build());

    pipeline2.setPipelineVariables(userVariables);

    when(pipelineService.readPipelineWithVariables(APP_ID, PIPELINE_ID)).thenReturn(pipeline2);
    Set<String> expressions = pipelineExpressionBuilder.getExpressions(APP_ID, PIPELINE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.size()).isEqualTo(1);
    assertThat(expressions).contains("workflow.variables.MyVar");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testGetDynamicExpressions() {
    Set<String> expressions = pipelineExpressionBuilder.getDynamicExpressions(APP_ID, PIPELINE_ID);
    assertThat(expressions).isEmpty();
  }
}
