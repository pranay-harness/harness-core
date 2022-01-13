/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.yaml.ManifestConfig.ManifestConfigStepParameters;
import static io.harness.cdng.manifest.yaml.ManifestOverrideSets.ManifestOverrideSetsStepParameters;
import static io.harness.cdng.manifest.yaml.kinds.K8sManifest.K8sManifestStepParameters;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.pms.yaml.validation.InputSetValidatorFactory;
import io.harness.rule.Owner;
import io.harness.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(CDP)
public class OverrideSetExpressionTest extends CategoryTest {
  @Mock private PlanExecutionService planExecutionService;
  @Inject private InputSetValidatorFactory inputSetValidatorFactory;

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testManifestOverrideSetExpressions() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/serviceConfig.yaml");
    ServiceConfig serviceConfig = YamlPipelineUtils.read(testFile, ServiceConfig.class);

    ManifestOverrideSets manifestOverrideSet =
        serviceConfig.getServiceDefinition().getServiceSpec().getManifestOverrideSets().get(0).getOverrideSet();
    assertThat(manifestOverrideSet.getIdentifier()).isEqualTo("overrideset1");
    assertThat(manifestOverrideSet.getManifests().size()).isEqualTo(1);

    ManifestOverrideSetsStepParameters manifestOverrideSetsStepParameters =
        ManifestOverrideSetsStepParameters.fromManifestOverrideSets(manifestOverrideSet);
    assertThat(manifestOverrideSetsStepParameters.containsKey("manifest1")).isEqualTo(true);
    assertThat(manifestOverrideSetsStepParameters.size()).isEqualTo(1);

    ManifestConfigStepParameters manifestConfigStepParameters = manifestOverrideSetsStepParameters.get("manifest1");
    assertThat(manifestConfigStepParameters.getIdentifier()).isEqualTo("manifest1");
    assertThat(manifestConfigStepParameters.getType()).isEqualTo(ManifestType.K8Manifest);

    K8sManifestStepParameters k8sSpec = (K8sManifestStepParameters) manifestConfigStepParameters.getSpec();
    assertThat(k8sSpec.getIdentifier()).isEqualTo("manifest1");
    assertThat(k8sSpec.getSkipResourceVersioning().getValue()).isEqualTo(null);
    assertThat(k8sSpec.getStore().getType()).isEqualTo(ManifestStoreType.GIT);

    GitStore gitStore = (GitStore) k8sSpec.getStore().getSpec();
    assertThat(gitStore.getConnectorRef().getValue()).isEqualTo("org.GitConnectorForAutomationTest");
    assertThat(gitStore.getGitFetchType().getName()).isEqualTo("Branch");
    assertThat(gitStore.getBranch().getValue()).isEqualTo("master");
    assertThat(gitStore.getCommitId().getValue()).isEqualTo(null);
    assertThat(gitStore.getPaths().getValue().size()).isEqualTo(1);
    assertThat(gitStore.getPaths().getValue().get(0)).isEqualTo("ng-automation/k8s/templates/");
    assertThat(gitStore.getFolderPath().getValue()).isEqualTo(null);
    assertThat(gitStore.getRepoName().getValue()).isEqualTo(null);
  }
}
