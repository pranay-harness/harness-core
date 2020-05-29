package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.CIPipeline;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;

public class YAMLToObjectImplTest extends CIManagerTest {
  @Mock private YamlPipelineUtils yamlPipelineUtils;
  @InjectMocks private YAMLToObjectImpl yamlToObject;

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReturnCIPipeline() {
    when(yamlPipelineUtils.read(anyString(), any())).thenReturn(CIPipeline.builder().build());
    String yaml = "dummy";
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml);
    assertThat(ciPipeline).isNotNull();

    verify(yamlPipelineUtils, times(1)).read(anyString(), any());
  }

  @SneakyThrows
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReturnNull() {
    when(yamlPipelineUtils.read(anyString(), any())).thenThrow(new IOException());
    String yaml = "dummy";
    CIPipeline ciPipeline = yamlToObject.convertYAML(yaml);
    assertThat(ciPipeline).isNull();
    verify(yamlPipelineUtils, times(1)).read(anyString(), any());
  }
}