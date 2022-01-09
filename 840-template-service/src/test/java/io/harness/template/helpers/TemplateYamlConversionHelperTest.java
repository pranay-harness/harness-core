/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.helpers;

import static io.harness.ng.core.template.TemplateEntityConstants.STAGE;
import static io.harness.ng.core.template.TemplateEntityConstants.STEP;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.TemplateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.rule.Owner;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.handler.DummyParallelYamlConversionHandler;
import io.harness.template.handler.DummyReplaceYamlConversionHandler;
import io.harness.template.handler.TemplateYamlConversionHandler;
import io.harness.template.handler.TemplateYamlConversionHandlerRegistry;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDC)
public class TemplateYamlConversionHelperTest extends TemplateServiceTestBase {
  @InjectMocks TemplateYamlConversionHelper templateYamlConversionHelper;
  @Mock TemplateYamlConversionHandlerRegistry templateYamlConversionHandlerRegistry;

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testConvertStepTemplateYamlToPMSUnderstandableYaml() {
    when(templateYamlConversionHandlerRegistry.obtain(STEP)).thenReturn(new TemplateYamlConversionHandler());
    String filename = "template-step.yaml";
    String yaml = readFile(filename);
    String newYaml = templateYamlConversionHelper.convertTemplateYamlToPMSUnderstandableYaml(
        TemplateEntity.builder().templateEntityType(TemplateEntityType.STEP_TEMPLATE).yaml(yaml).build());
    assertThat(newYaml).isNotNull();
    assertThat(newYaml).isEqualTo(readFile("pms-template-step.yaml"));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testConvertStageTemplateYamlToPMSUnderstandableYaml() {
    when(templateYamlConversionHandlerRegistry.obtain(STAGE)).thenReturn(new TemplateYamlConversionHandler());
    String filename = "stage-template.yaml";
    String yaml = readFile(filename);
    String newYaml = templateYamlConversionHelper.convertTemplateYamlToPMSUnderstandableYaml(
        TemplateEntity.builder().templateEntityType(TemplateEntityType.STAGE_TEMPLATE).yaml(yaml).build());
    assertThat(newYaml).isNotNull();
    assertThat(newYaml).isEqualTo(readFile("pms-stage-template.yaml"));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDummyParallelYamlConversionHandler() {
    when(templateYamlConversionHandlerRegistry.obtain(any())).thenReturn(new DummyParallelYamlConversionHandler());
    String filename = "stage-template.yaml";
    String yaml = readFile(filename);
    String newYaml = templateYamlConversionHelper.convertTemplateYamlToPMSUnderstandableYaml(
        TemplateEntity.builder().templateEntityType(TemplateEntityType.STAGE_TEMPLATE).yaml(yaml).build());
    assertThat(newYaml).isNotNull();
    assertThat(newYaml).isEqualTo(readFile("pms-dummy-parallel-stage-template.yaml"));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testDummyReplaceYamlConversionHandler() {
    when(templateYamlConversionHandlerRegistry.obtain(any())).thenReturn(new DummyReplaceYamlConversionHandler());
    String filename = "stage-template.yaml";
    String yaml = readFile(filename);
    String newYaml = templateYamlConversionHelper.convertTemplateYamlToPMSUnderstandableYaml(
        TemplateEntity.builder().templateEntityType(TemplateEntityType.STAGE_TEMPLATE).yaml(yaml).build());
    assertThat(newYaml).isNotNull();
    assertThat(newYaml).isEqualTo(readFile("pms-dummy-replace-stage-template.yaml"));
  }
}
