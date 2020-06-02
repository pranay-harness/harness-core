package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.CIPipeline;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.IOException;

public class YAMLToObjectImplTest extends CIManagerTest {
  @Spy private YAMLToObjectImpl yamlToObject;

  void init() {
    MockitoAnnotations.initMocks(this);
  }

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReturnCIPipeline() {
    doReturn(CIPipeline.builder().build()).when(yamlToObject).readYaml(anyString());
    String yaml = "dummy";
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml);
    assertThat(ciPipeline).isNotNull();

    verify(yamlToObject, times(1)).readYaml(anyString());
  }

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReturnNull() {
    doThrow(new IOException()).when(yamlToObject).readYaml(anyString());
    String yaml = "dummy";
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml);
    assertThat(ciPipeline).isNull();
    verify(yamlToObject, times(1)).readYaml(anyString());
  }
}