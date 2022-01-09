/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ResourceConstraint;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.service.intfc.ResourceConstraintService;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
public class ResourceConstraintStepYamlBuilderTest extends CategoryTest {
  private static final String RESOURCE_CONSTRAINT_ID = "resourceConstraintId";
  private static final String RESOURCE_CONSTRAINT_NAME = "resourceConstraintName";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String APP_ID = "APP_ID";

  @InjectMocks ResourceConstraintStepYamlBuilder resourceConstraintStepYamlBuilder;
  @Mock ResourceConstraintService resourceConstraintService;

  @Before
  public void setup() {
    initMocks(this);
    ResourceConstraint resourceConstraint =
        ResourceConstraint.builder().uuid(RESOURCE_CONSTRAINT_ID).name(RESOURCE_CONSTRAINT_NAME).build();
    when(resourceConstraintService.getById(RESOURCE_CONSTRAINT_ID)).thenReturn(resourceConstraint);
    when(resourceConstraintService.getByName(ACCOUNT_ID, RESOURCE_CONSTRAINT_NAME)).thenReturn(resourceConstraint);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> resourceConstraintStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> resourceConstraintStepYamlBuilder.convertIdToNameForKnownTypes(
                                    name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(true));
  }

  private Map<String, Object> getInputProperties(boolean withName) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(RESOURCE_CONSTRAINT_NAME, RESOURCE_CONSTRAINT_NAME);
    } else {
      inputProperties.put(RESOURCE_CONSTRAINT_ID, RESOURCE_CONSTRAINT_ID);
    }
    inputProperties.put("holdingScope", "WORKFLOW");
    inputProperties.put("permits", 100);
    inputProperties.put("timeoutMillis", 123);
    return inputProperties;
  }
}
