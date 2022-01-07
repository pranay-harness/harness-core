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
public class JiraStepYamlBuilderTest extends StepYamlBuilderTestBase {
  @InjectMocks private JiraStepYamlBuilder jiraStepYamlBuilder;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes() {
    Map<String, Object> inputProperties = getInputProperties(true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> jiraStepYamlBuilder.convertNameToIdForKnownTypes(
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
        (name, value) -> jiraStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties(true));
  }

  private Map<String, Object> getInputProperties(boolean withName) {
    Map<String, Object> inputProperties = new HashMap<>();
    if (withName) {
      inputProperties.put(JIRA_CONNECTOR_NAME, JIRA_CONNECTOR_NAME);
    } else {
      inputProperties.put(JIRA_CONNECTOR_ID, JIRA_CONNECTOR_ID);
    }
    inputProperties.put("jiraAction", "CREATE_TICKET");
    inputProperties.put("project", "TJI");
    inputProperties.put("issueType", "Story");
    inputProperties.put("summary", "Local Test");
    inputProperties.put("priority", "P0");
    return inputProperties;
  }
}
