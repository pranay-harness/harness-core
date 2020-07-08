package software.wings.service.impl.pipeline;

import static io.harness.rule.OwnerRule.DHRUV;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.EntityType.USER_GROUP;
import static software.wings.beans.PipelineStage.PipelineStageElement;
import static software.wings.beans.PipelineStage.PipelineStageElement.builder;
import static software.wings.service.impl.pipeline.PipelineServiceValidator.validateTemplateExpressions;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PipelineServiceValidatorTest extends CategoryTest {
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
}