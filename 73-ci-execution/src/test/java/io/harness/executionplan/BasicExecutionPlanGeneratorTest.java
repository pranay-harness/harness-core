package io.harness.executionplan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.node.BasicStepToExecutionNodeConverter;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.rule.Owner;
import io.harness.state.StepType;
import io.harness.yaml.core.Execution;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.Arrays;

public class BasicExecutionPlanGeneratorTest extends CIExecutionTest {
  @Inject private BasicExecutionPlanGenerator basicExecutionPlanGenerator;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private BasicStepToExecutionNodeConverter basicStepToExecutionNodeConverter;

  private static final String UUID = "UUID";
  private static final String NAME = "name";

  @Before
  public void setUp() {
    on(basicExecutionPlanGenerator).set("basicStepToExecutionNodeConverter", basicStepToExecutionNodeConverter);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGenerateExecutionPlan() {
    ExecutionNode executionNode = ExecutionNode.builder()
                                      .uuid(UUID)
                                      .name(NAME)
                                      .identifier(generateUuid())
                                      .stepType(StepType.builder().type("DUMMY").build())
                                      .build();

    when(basicStepToExecutionNodeConverter.convertStep(any(), any())).thenReturn(executionNode);
    Execution execution = ciExecutionPlanTestHelper.getExecution();
    Plan plan = basicExecutionPlanGenerator.generateExecutionPlan(execution);
    assertThat(plan.getNodes()).isEqualTo(Arrays.asList(executionNode, executionNode));
  }
}
