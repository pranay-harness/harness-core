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
import static software.wings.service.impl.pipeline.PipelineServiceValidator.validateTemplateExpressions;

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
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.RuntimeInputsConfig;
import software.wings.beans.Variable;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.UserGroupService;

import java.util.ArrayList;
import java.util.Arrays;
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
    RuntimeInputsConfig runtimeInputsConfig = RuntimeInputsConfig.builder().build();
    pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig, "ACCOUNT_ID");
    RuntimeInputsConfig runtimeInputsConfig2 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).build();
    assertThatThrownBy(
        () -> { pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig2, "ACCOUNT_ID"); })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout value should be greater than 1 secs");

    RuntimeInputsConfig runtimeInputsConfig3 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).timeout(950L).build();
    assertThatThrownBy(
        () -> { pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig3, "ACCOUNT_ID"); })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout value should be greater than 1 secs");

    RuntimeInputsConfig runtimeInputsConfig4 =
        RuntimeInputsConfig.builder().runtimeInputVariables(asList("var1", "var2")).timeout(2000L).build();
    assertThatThrownBy(
        () -> { pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig4, "ACCOUNT_ID"); })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Timeout Action cannot be null");

    RuntimeInputsConfig runtimeInputsConfig7 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(2000L)
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    assertThatThrownBy(
        () -> { pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig7, "ACCOUNT_ID"); })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User groups should be present for Notification");

    when(userGroupService.get(any(), any())).thenReturn(null);
    RuntimeInputsConfig runtimeInputsConfig5 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(2000L)
                                                   .userGroupIds(asList("UG_ID"))
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    assertThatThrownBy(
        () -> { pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig5, "ACCOUNT_ID"); })
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("User group not found for given Id: UG_ID");

    when(userGroupService.get(any(), any())).thenReturn(UserGroup.builder().build());
    RuntimeInputsConfig runtimeInputsConfig6 = RuntimeInputsConfig.builder()
                                                   .runtimeInputVariables(asList("var1", "var2"))
                                                   .timeout(2000L)
                                                   .userGroupIds(asList("UG_ID"))
                                                   .timeoutAction(RepairActionCode.END_EXECUTION)
                                                   .build();
    pipelineServiceValidator.validateRuntimeInputsConfig(runtimeInputsConfig6, "ACCOUNT_ID");
  }
}