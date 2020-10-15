package software.wings.service.impl.pipeline;

import static io.harness.rule.OwnerRule.DHRUV;
import static io.harness.rule.OwnerRule.POOJA;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.USER_GROUP;
import static software.wings.beans.PipelineStage.PipelineStageElement;
import static software.wings.beans.PipelineStage.PipelineStageElement.builder;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.service.impl.pipeline.PipelineServiceValidator.validateTemplateExpressions;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.interrupts.RepairActionCode;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.RuntimeInputsConfig;
import software.wings.beans.Variable;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.UserGroupService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class PipelineServiceValidatorTest extends WingsBaseTest {
  @Mock UserGroupService userGroupService;

  @InjectMocks @Inject PipelineServiceValidator pipelineServiceValidator;

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testValidateTemplateExpressions() {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(Variable.ENTITY_TYPE, USER_GROUP);
    HashMap<String, Object> propertiestest = new HashMap<>();
    HashMap<String, Object> metadatatest = new HashMap<>();
    HashMap<String, Object> metadatatestvalue = new HashMap<>();
    metadatatestvalue.put("entityType", "USER_GROUP");
    metadatatestvalue.put("relatedField", "");
    metadatatest.put("metadata", metadatatestvalue);
    HashMap<String, Object> values = new HashMap<>();
    values.put("expression", "${User_Group}");
    values.put("fieldName", "userGroups");
    values.put("metadata", metadatatest);
    ArrayList listValues = new ArrayList();
    listValues.add(values);
    propertiestest.put("templateExpressions", listValues);

    PipelineStageElement pipelineStageElement =
        builder().type("APPROVAL").name("test").properties(propertiestest).parallelIndex(0).build();
    PipelineStage pipelineStage =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement)).parallel(false).build();
    Pipeline pipeline = Pipeline.builder().pipelineStages(Arrays.asList(pipelineStage)).build();

    boolean valid = validateTemplateExpressions(pipeline);
    assertThat(valid).isEqualTo(true);
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testValidateTemplateExpressionsFails() {
    HashMap<String, Object> metadata = new HashMap<>();
    metadata.put(Variable.ENTITY_TYPE, USER_GROUP);
    HashMap<String, Object> propertiestest = new HashMap<>();
    HashMap<String, Object> metadatatest = new HashMap<>();
    HashMap<String, Object> metadatatestvalue = new HashMap<>();
    metadatatestvalue.put("entityType", "USER_GROUP");
    metadatatestvalue.put("relatedField", "");
    metadatatest.put("metadata", metadatatestvalue);
    HashMap<String, Object> values = new HashMap<>();
    values.put("expression", "${User_");
    values.put("fieldName", "userGroups");
    values.put("metadata", metadatatest);
    ArrayList listValues = new ArrayList();
    listValues.add(values);
    propertiestest.put("templateExpressions", listValues);
    PipelineStageElement pipelineStageElement =
        builder().type("APPROVAL").name("test").properties(propertiestest).parallelIndex(0).build();
    PipelineStage pipelineStage =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement)).parallel(false).build();
    Pipeline pipeline = Pipeline.builder().pipelineStages(Arrays.asList(pipelineStage)).build();
    boolean thrown = false;
    try {
      validateTemplateExpressions(pipeline);
    } catch (InvalidRequestException e) {
      thrown = true;
    }
    assertThat(thrown).isEqualTo(true);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void validateRuntimeInputsConfig() {
    PipelineStageElement pse1 = builder().build();
    assertThat(pipelineServiceValidator.validateRuntimeInputsConfig(pse1, "ACCOUNT_ID", Collections.emptyList()))
        .isTrue();

    pse1.setWorkflowVariables(ImmutableMap.of("var1", "${abc}"));
    assertThat(pipelineServiceValidator.validateRuntimeInputsConfig(pse1, "ACCOUNT_ID", Collections.emptyList()))
        .isTrue();

    pse1.setDisableAssertion("true");
    assertThat(pipelineServiceValidator.validateRuntimeInputsConfig(pse1, "ACCOUNT_ID", Collections.emptyList()))
        .isTrue();

    PipelineStageElement pipelineStageElement = builder().workflowVariables(ImmutableMap.of("var1", "${abc}")).build();
    RuntimeInputsConfig runtimeInputsConfig = RuntimeInputsConfig.builder().build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig);
    pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    RuntimeInputsConfig runtimeInputsConfig2 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig2);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout value should be greater than 1 minute");

    RuntimeInputsConfig runtimeInputsConfig3 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).timeout(950L).build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig3);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout value should be greater than 1 minute");

    RuntimeInputsConfig runtimeInputsConfig4 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).timeout(60001L).build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig4);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout Action cannot be null");

    RuntimeInputsConfig runtimeInputsConfig7 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(60001L)
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig7);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User groups should be present for Notification");

    when(userGroupService.get(any(), any())).thenReturn(null);
    RuntimeInputsConfig runtimeInputsConfig5 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(60001L)
                                                   .userGroupIds(asList("UG_ID"))
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig5);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID", Collections.emptyList());
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User group not found for given Id: UG_ID");

    when(userGroupService.get(any(), any())).thenReturn(UserGroup.builder().build());
    RuntimeInputsConfig runtimeInputsConfig6 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(60001L)
                                                   .userGroupIds(asList("UG_ID"))
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig6);

    pipelineServiceValidator.validateRuntimeInputsConfig(
        pipelineStageElement, "ACCOUNT_ID", Collections.singletonList(aVariable().name("var1").build()));
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void validateRuntimeInputsConfigVariableValuesEntityType() {
    PipelineStageElement pipelineStageElement = builder().workflowVariables(ImmutableMap.of("var1", "abc")).build();
    RuntimeInputsConfig runtimeInputsConfig = RuntimeInputsConfig.builder()
                                                  .runtimeInputVariables(asList("var1", "var2"))
                                                  .timeout(60001L)
                                                  .userGroupIds(asList("UG_ID"))
                                                  .timeoutAction(RepairActionCode.END_EXECUTION)
                                                  .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig);
    when(userGroupService.get(any(), any())).thenReturn(UserGroup.builder().build());
    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID",
          Collections.singletonList(aVariable().name("var1").entityType(EntityType.ENVIRONMENT).build()));
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Variable var1 is marked runtime but the value isnt a valid expression");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void validateRuntimeInputsConfigVariableValuesNonEntityType() {
    PipelineStageElement pipelineStageElement = builder().workflowVariables(ImmutableMap.of("var1", "abc")).build();
    RuntimeInputsConfig runtimeInputsConfig = RuntimeInputsConfig.builder()
                                                  .runtimeInputVariables(asList("var1", "var2"))
                                                  .timeout(60001L)
                                                  .userGroupIds(asList("UG_ID"))
                                                  .timeoutAction(RepairActionCode.END_EXECUTION)
                                                  .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig);
    when(userGroupService.get(any(), any())).thenReturn(UserGroup.builder().build());
    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(
          pipelineStageElement, "ACCOUNT_ID", Collections.singletonList(aVariable().name("var1").build()));
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Variable var1 is marked runtime but the value isnt a valid expression");

    PipelineStageElement pipelineStageElement2 =
        builder().workflowVariables(ImmutableMap.of("var1", "${app.name}")).build();
    pipelineStageElement2.setRuntimeInputsConfig(runtimeInputsConfig);

    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(
          pipelineStageElement2, "ACCOUNT_ID", Collections.singletonList(aVariable().name("var1").build()));
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Non entity var var1 is marked Runtime, the value should be a new variable expression");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(FunctionalTests.class)
  public void validateRuntimeInputsConfigVariableRelatedFieldRuntime() {
    PipelineStageElement pipelineStageElement = builder().workflowVariables(ImmutableMap.of("var1", "${abc}")).build();
    RuntimeInputsConfig runtimeInputsConfig = RuntimeInputsConfig.builder()
                                                  .runtimeInputVariables(asList("var1"))
                                                  .timeout(60001L)
                                                  .userGroupIds(asList("UG_ID"))
                                                  .timeoutAction(RepairActionCode.END_EXECUTION)
                                                  .build();
    pipelineStageElement.setRuntimeInputsConfig(runtimeInputsConfig);
    when(userGroupService.get(any(), any())).thenReturn(UserGroup.builder().build());
    assertThatThrownBy(() -> {
      pipelineServiceValidator.validateRuntimeInputsConfig(pipelineStageElement, "ACCOUNT_ID",
          Collections.singletonList(
              aVariable().name("var1").relatedField("var2").entityType(EntityType.ENVIRONMENT).build()));
    })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Variable var2 should be runtime as var1 is marked runtime");
  }
}