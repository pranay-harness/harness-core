/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.functional.appService;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.AZURE_WEB_APP_API_TEST;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CDFunctionalTests;
import io.harness.delegate.task.azure.appservice.webapp.response.DeploymentSlotData;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.InfrastructureDefinitionRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.infra.InfrastructureDefinition;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AzureAppServicesAPIFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private SettingGenerator settingGenerator;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private InfrastructureDefinition infrastructureDefinition;
  private Environment environment;
  private SettingAttribute azureCloudProvider;

  @Before
  public void setUp() {
    Owners owners = ownerManager.create();
    Application application =
        applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();

    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(seed, owners, AZURE_WEB_APP_API_TEST);
    assertThat(infrastructureDefinition).isNotNull();

    azureCloudProvider = settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    assertThat(azureCloudProvider).isNotNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(CDFunctionalTests.class)
  public void testWebAppsServices() {
    final String appId = environment.getAppId();

    List<String> webApps =
        InfrastructureDefinitionRestUtils.listWebApps(bearerToken, appId, infrastructureDefinition.getUuid());

    assertThat(webApps).isNotEmpty();
    assertThat(webApps.containsAll(Arrays.asList("functiona-test"))).isTrue();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(CDFunctionalTests.class)
  public void testWebAppsServicesSlots() {
    final String appId = environment.getAppId();

    List<DeploymentSlotData> slots = InfrastructureDefinitionRestUtils.listSlots(
        bearerToken, appId, "functiona-test", infrastructureDefinition.getUuid());

    assertThat(slots).isNotEmpty();
    slots.forEach(deploymentSlotData
        -> assertThat(Arrays.asList("functiona-test", "functiona-test-stage")).contains(deploymentSlotData.name));
  }
}
