/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
