package io.harness.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ReflectionUtilsTest {
  private class DummyBase { public String baseField; }

  private class Dummy extends DummyBase {
    public String field;
    public String inheritField;
  }

  @Test
  public void getFieldByNameTest() {
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "dummy")).isNull();
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "inheritField").getName()).isEqualTo("inheritField");
    assertThat(ReflectionUtils.getFieldByName(Dummy.class, "baseField").getName()).isEqualTo("baseField");
  }

  @Test
  public void getAllDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getAllDeclaredAndInheritedFields(Dummy.class)
                   .stream()
                   .filter(f -> f.getName().contains("ield"))
                   .count())
        .isEqualTo(3);
  }

  @Test
  public void getDeclaredAndInheritedFields() {
    assertThat(ReflectionUtils.getDeclaredAndInheritedFields(Dummy.class, f -> f.getName().endsWith("Field")))
        .hasSize(2);
  }
}
