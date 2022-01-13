/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;

import software.wings.infra.InfrastructureDefinition;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class InfraDefinitionsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject HarnesscliHelper harnesscliHelper;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;

  private final Seed seed = new Seed(0);

  private InfrastructureDefinition infrastructureDefinition;
  private String appId;
  private String envId;

  @Before
  public void setUp() throws IOException {
    Owners owners = ownerManager.create();
    infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, "GCP_KUBERNETES", bearerToken);
    appId = infrastructureDefinition.getAppId();
    envId = infrastructureDefinition.getEnvId();
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Enable once feature flag is enabled")
  public void getInfraDefinitionsTest() throws IOException {
    String command = "harness get infradefinitions --application " + appId + " --environment " + envId;
    List<String> getInfraDefOutput = harnesscliHelper.executeCLICommand(command);

    boolean newInfraDefListed = false;
    for (String s : getInfraDefOutput) {
      if (s.contains(infrastructureDefinition.getUuid())) {
        newInfraDefListed = true;
        break;
      }
    }
    assertThat(newInfraDefListed).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Enable once feature flag is enabled")
  public void getInfraDefinitionsWithWrongAppIdTest() throws IOException {
    String command = "harness get infradefinitions --application "
        + "wrongAppId"
        + " --environment " + envId;
    List<String> getInfraDefOutput = harnesscliHelper.getCLICommandError(command);

    assertThat(getInfraDefOutput.get(0).contains("User not authorized")).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("Enable once feature flag is enabled")
  public void getInfraDefinitionsWithWrongEnvIdTest() throws IOException {
    String command = "harness get infradefinitions --application " + appId + " --environment "
        + "wrongEnvId";
    List<String> getInfraDefOutput = harnesscliHelper.getCLICommandError(command);

    assertThat(getInfraDefOutput.get(0).contains("User not authorized")).isTrue();
  }
}
