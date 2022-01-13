/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.functional.vmss;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions.AZURE_VMSS_API_TEST;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_SUBSCRIPTION_ID;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BASE_SCALE_SET_LOAD_BALANCER_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BASE_SCALE_SET_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BLUE_GREEN_BALANCER_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BLUE_GREEN_PROD_BP_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BLUE_GREEN_STAGE_BP_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_SUBSCRIPTION_QA_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_VM_USERNAME;
import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.azure.model.VirtualMachineScaleSetData;
import io.harness.category.element.CDFunctionalTests;
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
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AzureVMSSAPIFunctionalTest extends AbstractFunctionalTest {
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

    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(seed, owners, AZURE_VMSS_API_TEST);
    assertThat(infrastructureDefinition).isNotNull();

    azureCloudProvider = settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    assertThat(azureCloudProvider).isNotNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testListLoadBalancerBackendPools() {
    final String appId = environment.getAppId();

    List<String> backendPoolNames = InfrastructureDefinitionRestUtils.getAzureLoadBalancerBackendPools(
        bearerToken, appId, infrastructureDefinition.getUuid(), AZURE_VMSS_BLUE_GREEN_BALANCER_NAME);

    assertThat(backendPoolNames).isNotEmpty();
    assertThat(backendPoolNames.containsAll(
                   Arrays.asList(AZURE_VMSS_BLUE_GREEN_STAGE_BP_NAME, AZURE_VMSS_BLUE_GREEN_PROD_BP_NAME)))
        .isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testListVirtualMachineScaleSets() {
    final String appId = environment.getAppId();

    Map<String, String> scaleSets = InfrastructureDefinitionRestUtils.listVirtualMachineScaleSets(
        bearerToken, appId, AZURE_SUBSCRIPTION_ID, AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP, azureCloudProvider.getUuid());

    assertThat(scaleSets).isNotNull();
    assertThat(scaleSets.containsValue(AZURE_VMSS_BASE_SCALE_SET_NAME)).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testListLoadBalancers() {
    final String appId = environment.getAppId();

    List<String> loadBalancers =
        InfrastructureDefinitionRestUtils.listLoadBalancers(bearerToken, appId, infrastructureDefinition.getUuid());

    assertThat(loadBalancers).isNotEmpty();
    assertThat(loadBalancers.containsAll(
                   Arrays.asList(AZURE_VMSS_BASE_SCALE_SET_LOAD_BALANCER_NAME, AZURE_VMSS_BLUE_GREEN_BALANCER_NAME)))
        .isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testGetAzureVirtualMachineScaleSetByName() {
    final String appId = environment.getAppId();

    VirtualMachineScaleSetData scaleSet = InfrastructureDefinitionRestUtils.getAzureVirtualMachineScaleSetByName(
        bearerToken, appId, AZURE_SUBSCRIPTION_ID, AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP, azureCloudProvider.getUuid(),
        AZURE_VMSS_BASE_SCALE_SET_NAME);

    assertThat(scaleSet).isNotNull();
    assertThat(scaleSet.getName()).isEqualTo(AZURE_VMSS_BASE_SCALE_SET_NAME);
    assertThat(scaleSet.getVirtualMachineAdministratorUsername()).isEqualTo(AZURE_VMSS_VM_USERNAME);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testGetAzureSubscriptions() {
    final String appId = environment.getAppId();

    Map<String, String> subscriptions =
        InfrastructureDefinitionRestUtils.getAzureSubscriptions(bearerToken, appId, azureCloudProvider.getUuid());

    assertThat(subscriptions).isNotNull();
    assertThat(subscriptions.containsValue(AZURE_VMSS_SUBSCRIPTION_QA_NAME)).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(CDFunctionalTests.class)
  public void testGetAzureResourceGroupsNames() {
    final String appId = environment.getAppId();

    List<String> resourceGroupsNames = InfrastructureDefinitionRestUtils.getAzureResourceGroupsNames(
        bearerToken, appId, azureCloudProvider.getUuid(), AZURE_SUBSCRIPTION_ID);

    assertThat(resourceGroupsNames).isNotEmpty();
    assertThat(resourceGroupsNames.contains(AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP)).isTrue();
  }
}
