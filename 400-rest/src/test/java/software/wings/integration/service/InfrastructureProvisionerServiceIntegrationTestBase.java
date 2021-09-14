/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration.service;

import software.wings.integration.IntegrationTestBase;
import software.wings.rules.Integration;
import software.wings.service.intfc.InfrastructureProvisionerService;

import com.google.inject.Inject;
import org.mockito.InjectMocks;

@Integration
public class InfrastructureProvisionerServiceIntegrationTestBase extends IntegrationTestBase {
  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;

  //  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = GEORGE)
  //  public void listForTaskTest() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final InfrastructureMappingBlueprint mappingBlueprint =
  //    infrastructureProvisioner.getMappingBlueprints().get(0);
  //
  //    List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.listByBlueprintDetails(
  //        infrastructureProvisioner.getApplicationId(), infrastructureProvisioner.getInfrastructureProvisionerType(),
  //        mappingBlueprint.getManifestByServiceId(), mappingBlueprint.getDeploymentType(),
  //        mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(1);
  //
  //    provisioners =
  //    infrastructureProvisionerService.listByBlueprintDetails(infrastructureProvisioner.getApplicationId(),
  //        infrastructureProvisioner.getInfrastructureProvisionerType(), generateUuid(),
  //        mappingBlueprint.getDeploymentType(), mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(0);
  //  }
}
