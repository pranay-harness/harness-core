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

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(CDC)
public class JenkinsStepYamlBuilderTest extends StepYamlBuilderTestBase {
  @InjectMocks private JenkinsStepYamlBuilder jenkinsStepYamlBuilder;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(true, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> jenkinsStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(false, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(false, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name,
            value) -> jenkinsStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(true, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_NameTemplatised() {
    Map<String, Object> inputProperties = getInputProperties(true, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> jenkinsStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(false, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_NameTemplatised() {
    Map<String, Object> inputProperties = getInputProperties(false, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name,
            value) -> jenkinsStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(true, true));
  }

  private Map<String, Object> getInputProperties(boolean withName, boolean isTemplatised) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(JENKINS_NAME, isTemplatised ? null : JENKINS_NAME);
    } else {
      inputProperties.put(JENKINS_ID, isTemplatised ? null : JENKINS_ID);
    }
    inputProperties.put("jobName", "${job}");
    inputProperties.put("jobNameAsExpression", "true");
    inputProperties.put("timeoutMillis", 123);
    if (isTemplatised) {
      inputProperties.put(TEMPLATE_EXPRESSIONS, getTemplateExpressions("${jenkins_id}", JENKINS_ID));
    }
    return inputProperties;
  }
}
