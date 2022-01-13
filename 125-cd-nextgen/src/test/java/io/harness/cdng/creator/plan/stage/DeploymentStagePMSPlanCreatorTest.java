/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.stage;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Scanner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class DeploymentStagePMSPlanCreatorTest extends CDNGTestBase {
  @Inject DeploymentStagePMSPlanCreator deploymentStagePMSPlanCreator;
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDependenciesForService() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/service.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField serviceField = YamlUtils.readTree(yaml);

    String serviceNodeId = serviceField.getNode().getUuid();
    Dependencies dependencies = deploymentStagePMSPlanCreator.getDependenciesForService(
        serviceField, serviceNodeId, PipelineInfrastructure.builder().build());
    assertThat(dependencies).isNotEqualTo(null);
    assertThat(dependencies.getDependenciesMap().containsKey(serviceNodeId)).isEqualTo(true);
    assertThat(dependencies.getDependencyMetadataMap()
                   .get(serviceNodeId)
                   .containsMetadata(YamlTypes.INFRASTRUCTURE_STEP_PARAMETERS))
        .isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAddCDExecutionDependencies() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/service.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField executionField = YamlUtils.readTree(yaml);

    String executionNodeId = executionField.getNode().getUuid();
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    deploymentStagePMSPlanCreator.addCDExecutionDependencies(planCreationResponseMap, executionField);
    assertThat(planCreationResponseMap.containsKey(executionNodeId)).isEqualTo(true);
  }
}
