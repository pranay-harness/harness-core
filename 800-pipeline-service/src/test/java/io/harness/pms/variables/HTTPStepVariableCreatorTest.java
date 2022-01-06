/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.variables;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class HTTPStepVariableCreatorTest extends CategoryTest {
  String emptyVariablesAndHeaders = "pipeline:\n"
      + "  step:\n"
      + "    name: search\n"
      + "    identifier: search\n"
      + "    type: Http\n"
      + "    timeout: 10m\n"
      + "    spec:\n"
      + "      url: uuid1\n"
      + "      method: uuid2\n"
      + "      headers: []\n"
      + "      outputVariables: []\n"
      + "      requestBody: uuid3\n"
      + "      assertion: uuid4";

  String bareMinimum = "pipeline:\n"
      + "  step:\n"
      + "    name: search\n"
      + "    identifier: search\n"
      + "    type: Http\n"
      + "    timeout: 10m\n"
      + "    spec:\n"
      + "      url: uuid1\n"
      + "      method: uuid2";

  String headers = "pipeline:\n"
      + "  step:\n"
      + "    name: search\n"
      + "    identifier: search\n"
      + "    type: Http\n"
      + "    timeout: 10m\n"
      + "    spec:\n"
      + "      url: uuid1\n"
      + "      method: uuid2\n"
      + "      headers:\n"
      + "        - key: aq\n"
      + "          value: uuid3\n"
      + "        - key: aw\n"
      + "          value: uuid4";

  String missingHeaderKey = "pipeline:\n"
      + "  step:\n"
      + "    name: search\n"
      + "    identifier: search\n"
      + "    type: Http\n"
      + "    timeout: 10m\n"
      + "    spec:\n"
      + "      url: uuid1\n"
      + "      method: uuid2\n"
      + "      headers:\n"
      + "        - key: aq\n"
      + "          value: uuid3\n"
      + "        - value: uuid4";

  String outputVariables = "pipeline:\n"
      + "  step:\n"
      + "    name: search\n"
      + "    identifier: search\n"
      + "    type: Http\n"
      + "    timeout: 10m\n"
      + "    spec:\n"
      + "      url: uuid1\n"
      + "      method: uuid2\n"
      + "      outputVariables:\n"
      + "        - name: m\n"
      + "          value: uuid3\n"
      + "          type: String\n"
      + "          __uuid: eh";
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetSupportedStepTypes() {
    Set<String> supportedStepTypes = new HTTPStepVariableCreator().getSupportedStepTypes();
    assertThat(supportedStepTypes).hasSize(1);
    assertThat(supportedStepTypes).contains("Http");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testAddVariablesInComplexObject() throws IOException {
    HTTPStepVariableCreator variableCreator = new HTTPStepVariableCreator();

    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    Map<String, YamlOutputProperties> yamlOutputPropertiesMap = new HashMap<>();

    YamlNode step1 = getStepNode(emptyVariablesAndHeaders);
    variableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step1);
    assertThat(yamlOutputPropertiesMap).hasSize(0);
    assertThat(yamlPropertiesMap).hasSize(4);
    assertThat(yamlPropertiesMap).containsKeys("uuid1", "uuid2", "uuid3", "uuid4");

    yamlPropertiesMap.clear();
    yamlOutputPropertiesMap.clear();
    YamlNode step2 = getStepNode(bareMinimum);
    variableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step2);
    assertThat(yamlOutputPropertiesMap).hasSize(0);
    assertThat(yamlPropertiesMap).hasSize(2);
    assertThat(yamlPropertiesMap).containsKeys("uuid1", "uuid2");

    yamlPropertiesMap.clear();
    yamlOutputPropertiesMap.clear();
    YamlNode step3 = getStepNode(headers);
    variableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step3);
    assertThat(yamlOutputPropertiesMap).hasSize(0);
    assertThat(yamlPropertiesMap).hasSize(4);
    assertThat(yamlPropertiesMap).containsKeys("uuid1", "uuid2", "aq", "aw");

    yamlPropertiesMap.clear();
    yamlOutputPropertiesMap.clear();
    YamlNode step4 = getStepNode(missingHeaderKey);
    assertThatThrownBy(
        () -> variableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step4))
        .isInstanceOf(InvalidRequestException.class);

    yamlPropertiesMap.clear();
    yamlOutputPropertiesMap.clear();
    YamlNode step5 = getStepNode(outputVariables);
    variableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step5);
    assertThat(yamlOutputPropertiesMap).hasSize(1);
    assertThat(yamlOutputPropertiesMap).containsKeys("uuid3");
    assertThat(yamlPropertiesMap).hasSize(2);
    assertThat(yamlPropertiesMap).containsKeys("uuid1", "uuid2");
  }

  private YamlNode getStepNode(String yaml) throws IOException {
    return YamlUtils.readTree(yaml)
        .getNode()
        .getField("pipeline")
        .getNode()
        .getField("step")
        .getNode()
        .getField("spec")
        .getNode();
  }
}
