package io.harness.pms.serializer.recaster;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.core.Recaster;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.ParameterFieldValueWrapper;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParameterFieldRecastTransformerTest extends CategoryTest {
  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithObsoleteStructure() {
    Document document = new Document()
                            .append(Recaster.RECAST_CLASS_KEY, ParameterField.class.getName())
                            .append("expressionValue", "someValue")
                            .append("expression", false)
                            .append("typeString", false);
    ParameterField recasted = RecastOrchestrationUtils.fromDocument(document, ParameterField.class);
    assertThat(recasted.getExpressionValue()).isEqualTo("someValue");
    assertThat(recasted.isExpression()).isFalse();
    assertThat(recasted.isTypeString()).isFalse();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithValueField() {
    ParameterField<Object> parameterField = ParameterField.createValueField("Value");
    Document document = RecastOrchestrationUtils.toDocument(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromDocument(document, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithExpressionField() {
    ParameterField<Object> parameterField =
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).metaData>", null, true);
    Document document = RecastOrchestrationUtils.toDocument(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromDocument(document, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithJsonResponseField() {
    ParameterField<Object> parameterField = ParameterField.createJsonResponseField("{response: \"success\"}");
    Document document = RecastOrchestrationUtils.toDocument(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromDocument(document, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithInputSetValidator() {
    ParameterField<Object> parameterField = ParameterField.createValueFieldWithInputSetValidator(
        "Value", new InputSetValidator(InputSetValidatorType.REGEX, "*"), true);
    Document document = RecastOrchestrationUtils.toDocument(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromDocument(document, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithComplexObjects() {
    DummyB dummyB = DummyB.builder()
                        .strVal("a")
                        .intVal(1)
                        .listVal(Collections.singletonList("b"))
                        .mapVal(Collections.singletonMap("c", 3))
                        .build();
    ParameterFieldValueWrapper<List<DummyB>> l = new ParameterFieldValueWrapper<>(Collections.singletonList(dummyB));
    Document documentB = RecastOrchestrationUtils.toDocument(l);
    ParameterFieldValueWrapper recastedB =
        RecastOrchestrationUtils.fromDocument(documentB, ParameterFieldValueWrapper.class);
    assertThat(recastedB).isNotNull();

    //    DummyA dummyA =
    //    DummyA.builder().pf(ParameterField.createValueField(Collections.singletonList(dummyB))).build(); Document
    //    document = RecastOrchestrationUtils.toDocument(dummyA); DummyA recasted =
    //    RecastOrchestrationUtils.fromDocument(document, DummyA.class); assertThat(recasted).isNotNull();
  }

  @Data
  @Builder
  public static class DummyA {
    private ParameterField<List<DummyB>> pf;
  }

  @Data
  @Builder
  public static class DummyB {
    private String strVal;
    private int intVal;
    private List<String> listVal;
    private Map<String, Integer> mapVal;
  }
}
