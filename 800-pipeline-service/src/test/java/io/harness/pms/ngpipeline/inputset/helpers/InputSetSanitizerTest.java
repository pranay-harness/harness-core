/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.ngpipeline.inputset.helpers.InputSetSanitizer.sanitizeInputSet;
import static io.harness.pms.ngpipeline.inputset.helpers.InputSetSanitizer.sanitizeRuntimeInput;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class InputSetSanitizerTest extends CategoryTest {
  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testSanitizeInputSets() {
    String filename = "pipeline-extensive.yml";
    String yaml = readFile(filename);

    String wrongRuntimeInputFile = "runtimeInputWrong1.yml";
    String wrongRuntimeInput = readFile(wrongRuntimeInputFile);

    String sanitizedYaml1 = sanitizeRuntimeInput(yaml, wrongRuntimeInput);

    String inputSetWrongFile = "inputSetWrong1.yml";
    String inputSetWrongYaml = readFile(inputSetWrongFile);

    String sanitizedYaml2 = sanitizeInputSet(yaml, inputSetWrongYaml);

    String correctFile = "runtimeInput1.yml";
    String correctYaml = readFile(correctFile).replace("\"", "");
    assertThat(sanitizedYaml1.replace("\"", "")).isEqualTo(correctYaml);
    assertThat(sanitizedYaml2.replace("\"", "")).isEqualTo(correctYaml);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testTrimmedValues() {
    String yamlFile = "pipeline-with-space-values.yaml";
    String pipelineYaml = readFile(yamlFile);

    String trimYamlFile = "pipeline-with-trimmed-values.yaml";
    String trimmedExpectedPipelineYaml = readFile(trimYamlFile);

    String trimmedActualPipelineYaml = InputSetSanitizer.trimValues(pipelineYaml);
    assertThat(trimmedActualPipelineYaml).isEqualTo(trimmedExpectedPipelineYaml);
  }
}
