/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDC)
public class GcbStepYamlBuilder2Test extends StepYamlBuilderTestBase {
  @InjectMocks private GcbStepYamlBuilder gcbStepYamlBuilder;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_havingInlineBuild() {
    Map<String, Object> inputProperties = getInputProperties_havingInlineBuild(true, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> gcbStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties_havingInlineBuild(false, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_havingInlineBuild() {
    Map<String, Object> inputProperties = getInputProperties_havingInlineBuild(false, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value) -> gcbStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties_havingInlineBuild(true, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_havingInlineBuild_withGcpConfigTemplatised() {
    Map<String, Object> inputProperties = getInputProperties_havingInlineBuild(true, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> gcbStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties_havingInlineBuild(false, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_havingInlineBuild_withGcpConfigTemplatised() {
    Map<String, Object> inputProperties = getInputProperties_havingInlineBuild(false, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value) -> gcbStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties).containsExactlyInAnyOrderEntriesOf(getInputProperties_havingInlineBuild(true, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_havingRemoteBuild() {
    Map<String, Object> inputProperties = getInputProperties_havingRemoteBuild(true, false, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> gcbStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties)
        .containsExactlyInAnyOrderEntriesOf(getInputProperties_havingRemoteBuild(false, false, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_havingRemoteBuild() {
    Map<String, Object> inputProperties = getInputProperties_havingRemoteBuild(false, false, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value) -> gcbStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties)
        .containsExactlyInAnyOrderEntriesOf(getInputProperties_havingRemoteBuild(true, false, true));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testNameToIdForKnownTypes_havingRemoteBuild_withGitConfigTemplatised() {
    Map<String, Object> inputProperties = getInputProperties_havingRemoteBuild(true, true, true);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach((name, value)
                                -> gcbStepYamlBuilder.convertNameToIdForKnownTypes(
                                    name, value, outputProperties, APP_ID, ACCOUNT_ID, null));
    assertThat(outputProperties)
        .containsExactlyInAnyOrderEntriesOf(getInputProperties_havingRemoteBuild(false, true, false));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testIdToNameForKnownTypes_havingRemoteBuild_withGitConfigTemplatised() {
    Map<String, Object> inputProperties = getInputProperties_havingRemoteBuild(false, true, false);
    Map<String, Object> outputProperties = new HashMap<>();

    inputProperties.forEach(
        (name, value) -> gcbStepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, APP_ID, null));
    assertThat(outputProperties)
        .containsExactlyInAnyOrderEntriesOf(getInputProperties_havingRemoteBuild(true, true, true));
  }

  private Map<String, Object> getInputProperties_havingInlineBuild(boolean withName, boolean isTemplatised) {
    Map<String, Object> inputProperties = new HashMap<>();
    Map<String, Object> gcbOptions = getGcbOptionsWithInlineBuild();
    setInputProperties(withName, isTemplatised, inputProperties, gcbOptions);
    return inputProperties;
  }

  private Map<String, Object> getInputProperties_havingRemoteBuild(
      boolean withRemoteRepoName, boolean isRemoteRepoTemplatised, boolean withGcpConfigName) {
    Map<String, Object> inputProperties = new HashMap<>();
    Map<String, Object> gcbOptions = getGcbOptionsWithRemoteBuild(withRemoteRepoName, isRemoteRepoTemplatised);
    setInputProperties(withGcpConfigName, false, inputProperties, gcbOptions);
    return inputProperties;
  }

  private void setInputProperties(
      boolean withName, boolean isTemplatised, Map<String, Object> inputProperties, Map<String, Object> gcbOptions) {
    gcbOptions.put(withName ? GCP_CONFIG_NAME : GCP_CONFIG_ID,
        isTemplatised ? null : (withName ? GCP_CONFIG_NAME : GCP_CONFIG_ID));
    inputProperties.put("sweepingOutputName", null);
    inputProperties.put(GCB_OPTIONS, gcbOptions);
    inputProperties.put("executeWithPreviousSteps", "false");
    inputProperties.put("timeoutMillis", 123);
    if (isTemplatised) {
      inputProperties.put(TEMPLATE_EXPRESSIONS, getTemplateExpressions("${gcp_config}", GCP_CONFIG_ID));
    }
  }

  private Map<String, Object> getGcbOptionsWithInlineBuild() {
    Map<String, Object> gcbOptions = new HashMap<>();
    gcbOptions.put("specSource", "INLINE");
    gcbOptions.put("inlineSpec", "a:b");
    return gcbOptions;
  }

  private Map<String, Object> getGcbOptionsWithRemoteBuild(
      boolean withRemoteRepoName, boolean isRemoteRepoTemplatised) {
    Map<String, Object> gcbOptions = new HashMap<>();
    gcbOptions.put("specSource", "REMOTE");
    gcbOptions.put("repositorySpec", "a:b");
    Map<String, String> repositorySpec = new HashMap<>();
    repositorySpec.put("filePath", "master");
    repositorySpec.put("repoName", "");
    repositorySpec.put("fileSource", "BRANCH");
    repositorySpec.put("sourceId", "master");
    repositorySpec.put(withRemoteRepoName ? GIT_CONFIG_NAME : GIT_CONFIG_ID,
        isRemoteRepoTemplatised ? null : (withRemoteRepoName ? GIT_CONFIG_NAME : GIT_CONFIG_ID));
    gcbOptions.put(REPOSITORY_SPEC, repositorySpec);
    return gcbOptions;
  }
}
