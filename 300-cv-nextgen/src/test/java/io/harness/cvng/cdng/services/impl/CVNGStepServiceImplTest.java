/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.cdng.services.api.CVNGStepService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGStepServiceImplTest extends CvNextGenTestBase {
  private static List<YamlTest> YAML_TEMPLATE_API_TESTS =
      Arrays.asList(YamlTest.builder()
                        .pipelineYamlFile("pipeline/inputset/pipeline1.yaml")
                        .outputYamlFile("pipeline/inputset/output-template1.yaml")
                        .build(),
          YamlTest.builder()
              .pipelineYamlFile("pipeline/inputset/pipeline2.yaml")
              .outputYamlFile("pipeline/inputset/output-template2.yaml")
              .build(),
          YamlTest.builder()
              .pipelineYamlFile("pipeline/inputset/bluegreen-canary-pipeline.yaml")
              .outputYamlFile("pipeline/inputset/bluegreen-canary-output-template.yaml")
              .build(),
          YamlTest.builder().pipelineYamlFile("pipeline/inputset/pipeline-with-fixed-service-env.yaml").build());
  @Inject private CVNGStepService cvngStepService;
  @Before
  public void setup() throws IllegalAccessException {}
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetUpdatedInputSetTemplate() throws IOException {
    for (YamlTest test : YAML_TEMPLATE_API_TESTS) {
      String pipelineYaml = readYamlFile(test.getPipelineYamlFile());
      if (test.getOutputYamlFile() == null) {
        assertThat(cvngStepService.getUpdatedInputSetTemplate(pipelineYaml)).isNull();
      } else {
        assertThat(YamlUtils.readTree(cvngStepService.getUpdatedInputSetTemplate(pipelineYaml)))
            .isEqualTo(YamlUtils.readTree(readYamlFile(test.getOutputYamlFile())));
      }
    }
  }
  @Value
  @Builder
  private static class YamlTest {
    String pipelineYamlFile;
    String outputYamlFile;
  }

  private String readYamlFile(String yamlPath) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource(yamlPath);
    return Resources.toString(testFile, Charsets.UTF_8);
  }
}
