/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.app;

import static io.harness.rule.OwnerRule.BOGDAN;

import static org.assertj.core.api.Assertions.fail;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class MainConfigurationConfigSecretTest {
  private static final Class<MainConfiguration> ROOT_CONFIG_CLASS = MainConfiguration.class;

  /**
   * Similar to {@link io.harness.ng.NextGenConfigurationSecretTest}
   */
  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void configSecretsShouldHaveAllParentsMarkWithConfigSecretAnnotation() {
    List<List<String>> fails = Lists.newArrayList();
    for (Field field : FieldUtils.getFieldsListWithAnnotation(ROOT_CONFIG_CLASS, JsonProperty.class)) {
      boolean isSecret = isAnnotatedWithConfigSecret(field);

      if (couldBeConfigClass(field)) {
        fails.addAll(traverse(field, !isSecret, ImmutableList.of(ROOT_CONFIG_CLASS.getName(), field.getName())));
      }
    }

    if (!fails.isEmpty()) {
      StringBuilder errorMessageBuilder = new StringBuilder("Following paths failed: \n ");
      for (List<String> failElement : fails) {
        errorMessageBuilder.append("  ").append(failElement).append(System.lineSeparator());
      }
      String errorMessage = errorMessageBuilder.toString();
      fail(errorMessage);
    }
  }

  private List<List<String>> traverse(Field field, boolean pathLacksMarker, List<String> pathRoute) {
    List<List<String>> currentFails = new ArrayList<>();
    for (Field subfield : FieldUtils.getAllFields(field.getType())) {
      boolean subfieldMarked = isAnnotatedWithConfigSecret(subfield);

      if (pathLacksMarker && subfieldMarked) {
        List<String> failPath = Lists.newArrayList(pathRoute);
        failPath.add(subfield.getName());
        currentFails.add(failPath);
      }

      if (couldBeConfigClass(subfield)) {
        boolean subPathLacksMarker = pathLacksMarker || !subfieldMarked;
        List<String> newParentTypes = Lists.newArrayList(pathRoute);
        newParentTypes.add(subfield.getType().getTypeName());
        currentFails.addAll(traverse(subfield, subPathLacksMarker, newParentTypes));
      }
    }
    return currentFails;
  }

  private boolean isAnnotatedWithConfigSecret(Field field) {
    return field.isAnnotationPresent(ConfigSecret.class);
  }

  private boolean couldBeConfigClass(Field field) {
    return !field.getType().isEnum()
        && (field.getType().getName().startsWith("io.harness")
            || field.getType().getName().startsWith("software.wings"));
  }
}
