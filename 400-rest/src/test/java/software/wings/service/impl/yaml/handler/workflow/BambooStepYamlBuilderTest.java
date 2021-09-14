/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(CDC)
public class BambooStepYamlBuilderTest extends StepYamlBuilderTestBase {
  @InjectMocks BambooStepYamlBuilder bambooStepYamlBuilder;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> bambooStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name,
            value) -> bambooStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(true));
  }

  private Map<String, Object> getInputProperties(boolean withName) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(BAMBOO_CONFIG_NAME, BAMBOO_CONFIG_NAME);
    } else {
      inputProperties.put(BAMBOO_CONFIG_ID, BAMBOO_CONFIG_ID);
    }
    inputProperties.put("parameters", null);
    inputProperties.put("planName", "ABC");
    inputProperties.put("timeoutMillis", 123);
    return inputProperties;
  }
}
