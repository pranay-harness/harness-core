package io.harness.pms.sdk.core.execution;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import lombok.Builder;
import lombok.Data;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NodeExecutionUtilsTest extends PmsSdkCoreTestBase {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testResolveObject() {
    Dummy dummyInner = Dummy.builder()
                           .strVal1("a")
                           .intVal1(1)
                           .strVal2(ParameterField.createValueField("b"))
                           .intVal2(ParameterField.createExpressionField(true, "<+tmp1>", null, false))
                           .dummy(ParameterField.createExpressionField(true, "<+tmp2>", null, false))
                           .build();
    Dummy dummy = Dummy.builder()
                      .strVal1("c")
                      .intVal1(2)
                      .strVal2(ParameterField.createExpressionField(true, "<+tmp3>", null, true))
                      .intVal2(ParameterField.createValueField(3))
                      .dummy(ParameterField.createValueField(dummyInner))
                      .build();

    Document doc = RecastOrchestrationUtils.toDocument(dummy);
    Object resp = NodeExecutionUtils.resolveObject(doc);
    assertThat(resp).isNotNull();
    assertThat(resp).isInstanceOf(Document.class);

    doc = (Document) resp;
    assertThat(doc.get("strVal1")).isEqualTo("c");
    assertThat(doc.get("intVal1")).isEqualTo(2);
    assertThat(doc.get("strVal2")).isEqualTo("<+tmp3>");
    assertThat(doc.get("intVal2")).isEqualTo(3);

    doc = (Document) doc.get("dummy");
    assertThat(doc).isNotNull();
    assertThat(doc.get("strVal1")).isEqualTo("a");
    assertThat(doc.get("intVal1")).isEqualTo(1);
    assertThat(doc.get("strVal2")).isEqualTo("b");
    assertThat(doc.get("intVal2")).isEqualTo("<+tmp1>");
    assertThat(doc.get("dummy")).isEqualTo("<+tmp2>");
  }

  @Data
  @Builder
  private static class Dummy {
    private String strVal1;
    private Integer intVal1;
    private ParameterField<String> strVal2;
    private ParameterField<Integer> intVal2;
    private ParameterField<Dummy> dummy;
  }
}
