/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.inframapping;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class AwsLambdaInfraMappingTest extends WingsBaseTest {
  private AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping;

  @Before
  public void setUp() throws Exception {
    awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    Map<String, Object> outputVariablesMap = new HashMap<>();

    outputVariablesMap.put("region", "testRegion");
    outputVariablesMap.put("role", "testRole");
    outputVariablesMap.put("vpcId", "testVpc");

    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP, false);
    assertThat(awsLambdaInfraStructureMapping.getRegion()).isEqualTo("testRegion");
    assertThat(awsLambdaInfraStructureMapping.getRole()).isEqualTo("testRole");
    assertThat(awsLambdaInfraStructureMapping.getVpcId()).isEqualTo("testVpc");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testInvalidKey() {
    Map<String, Object> outputVariablesMap = new HashMap<>();

    outputVariablesMap.put("region1", "testRegion");
    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP, false);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testMandatoryFields() {
    Map<String, Object> outputVariablesMap = new HashMap<>();
    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP, false);
  }
}
