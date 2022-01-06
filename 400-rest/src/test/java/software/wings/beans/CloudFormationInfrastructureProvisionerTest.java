/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.CloudFormationInfrastructureProvisioner.CloudFormationInfrastructureProvisionerBuilder;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class CloudFormationInfrastructureProvisionerTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testCloudFormationProvisioner() {
    CloudFormationInfrastructureProvisionerBuilder builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_BODY");
    assertThat(builder.build().provisionByBody()).isTrue();
    builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("TEMPLATE_URL");
    assertThat(builder.build().provisionByUrl()).isTrue();
    builder = CloudFormationInfrastructureProvisioner.builder();
    builder.sourceType("GIT");
    assertThat(builder.build().provisionByGit()).isTrue();
  }
}
