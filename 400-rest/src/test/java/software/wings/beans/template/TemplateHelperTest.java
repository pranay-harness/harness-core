/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import software.wings.service.impl.template.TemplateBaseTestHelper;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TemplateHelperTest extends TemplateBaseTestHelper {
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_mappedEntity() {
    final List<TemplateType> supportedTemplateTypes = Arrays.asList(TemplateType.HTTP, TemplateType.SHELL_SCRIPT,
        TemplateType.ARTIFACT_SOURCE, TemplateType.SSH, TemplateType.PCF_PLUGIN, TemplateType.CUSTOM_DEPLOYMENT_TYPE);
    for (TemplateType supportedTemplateType : supportedTemplateTypes) {
      assertThat(TemplateHelper.mappedEntities(supportedTemplateType)).isNotNull();
    }

    Arrays.stream(TemplateType.values())
        .filter(templateType -> !supportedTemplateTypes.contains(templateType))
        .forEach(templateType
            -> assertThatExceptionOfType(InvalidArgumentsException.class)
                   .isThrownBy(() -> TemplateHelper.mappedEntities(templateType)));
  }
}
