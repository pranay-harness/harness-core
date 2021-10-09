package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.FieldProperty;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class YamlRepresenterTest extends CategoryTest {
  YamlRepresenter representerRemoveEmpty = new YamlRepresenter(true);

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void representJavaBeanPropertyKeepEmptyAsIs() throws NoSuchFieldException {
    TestClass target = TestClass.builder().name("ABC").dontHideMeIfEmpty("").hideMeIfEmpty("").valueType(null).build();

    NodeTuple tuple;
    tuple = representerRemoveEmpty.representJavaBeanProperty(
        target, new FieldProperty(TestClass.class.getField("dontHideMeIfEmpty")), target.getDontHideMeIfEmpty(), null);
    assertThat(tuple).isNotNull();

    tuple = representerRemoveEmpty.representJavaBeanProperty(
        target, new FieldProperty(TestClass.class.getField("hideMeIfEmpty")), target.getHideMeIfEmpty(), null);
    assertThat(tuple).isNull();

    tuple = representerRemoveEmpty.representJavaBeanProperty(
        target, new FieldProperty(TestClass.class.getField("valueType")), target.getValueType(), null);
    assertThat(tuple).isNull();
  }

  @Value
  @Builder
  static class TestClass {
    public String name;
    @YamlKeepEmptyAsIs public String dontHideMeIfEmpty;
    public String hideMeIfEmpty;
    @YamlKeepEmptyAsIs public String valueType;
  }
}
