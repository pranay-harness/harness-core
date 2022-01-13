/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.amazonaws.services.ecs.model.LaunchType;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EcsInfrastructureMappingTest extends WingsBaseTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    EcsInfrastructureMapping infrastructureMapping = new EcsInfrastructureMapping();
    Map<String, Object> resolvedBlueprints = new HashMap<>();
    EcsInfrastructureMapping finalInfrastructureMapping4 = infrastructureMapping;
    Assertions
        .assertThatThrownBy(() -> finalInfrastructureMapping4.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put("region", "fda");
    resolvedBlueprints.put("clusterName", "fda");
    resolvedBlueprints.put("vpcId", "fda");
    resolvedBlueprints.put("subnetIds", "fda");
    resolvedBlueprints.put("securityGroupIds", "fda");
    resolvedBlueprints.put("executionRole", "fda");
    infrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null, true);

    resolvedBlueprints.remove("clusterName");
    Assertions
        .assertThatThrownBy(
            () -> new EcsInfrastructureMapping().applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put("clusterName", "fda");
    resolvedBlueprints.remove("vpcId");
    infrastructureMapping = new EcsInfrastructureMapping();
    infrastructureMapping.setLaunchType(LaunchType.FARGATE.toString());
    EcsInfrastructureMapping finalInfrastructureMapping = infrastructureMapping;
    Assertions
        .assertThatThrownBy(() -> finalInfrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put("vpcId", "fda");
    resolvedBlueprints.remove("subnetIds");
    infrastructureMapping = new EcsInfrastructureMapping();
    infrastructureMapping.setLaunchType(LaunchType.FARGATE.toString());
    EcsInfrastructureMapping finalInfrastructureMapping1 = infrastructureMapping;
    Assertions
        .assertThatThrownBy(() -> finalInfrastructureMapping1.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put("subnetIds", "fda");
    resolvedBlueprints.remove("securityGroupIds");
    infrastructureMapping = new EcsInfrastructureMapping();
    infrastructureMapping.setLaunchType(LaunchType.FARGATE.toString());
    EcsInfrastructureMapping finalInfrastructureMapping2 = infrastructureMapping;
    Assertions
        .assertThatThrownBy(() -> finalInfrastructureMapping2.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put("securityGroupIds", "fda");
    resolvedBlueprints.remove("executionRole");
    infrastructureMapping = new EcsInfrastructureMapping();
    infrastructureMapping.setLaunchType(LaunchType.FARGATE.toString());
    EcsInfrastructureMapping finalInfrastructureMapping3 = infrastructureMapping;
    Assertions
        .assertThatThrownBy(() -> finalInfrastructureMapping3.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);
  }
}
