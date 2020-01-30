package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V3;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.dl.WingsPersistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class ServiceResourceServiceImplTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceServiceImpl serviceResourceService;
  private ServiceResourceServiceImpl spyServiceResourceService = spy(new ServiceResourceServiceImpl());

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldSaveWithDefaultHelm2Version() {
    Service helmService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    serviceResourceService.checkAndSetHelmVersion(helmService);
    assertThat(helmService.getHelmVersion()).isEqualTo(V2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldSaveWithHelm3Version() {
    Service helmService = Service.builder().deploymentType(HELM).helmVersion(V3).build();
    serviceResourceService.checkAndSetHelmVersion(helmService);
    assertThat(helmService.getHelmVersion()).isEqualTo(V3);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldValidateHelmVersionAndDeploymentType() {
    shouldFailIfVersionSetForNonHelmK8sService();
  }

  private void shouldFailIfVersionSetForNonHelmK8sService() {
    Service service =
        Service.builder().appId(APP_ID).uuid("some-random-id").deploymentType(SSH).helmVersion(V2).build();

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> serviceResourceService.validateHelmVersion(service))
        .withMessageContaining("helmVersion is only supported with Helm and Kubernetes type of services");
  }

  private void shouldBeNoopIfHelmVersionNotSet() {
    Service service = Service.builder().appId(APP_ID).uuid("some-random-id").deploymentType(SSH).build();
    serviceResourceService.validateHelmVersion(service);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldUpdateHelmVersionIfNotPresent() {
    Service helmService = Service.builder().appId(APP_ID).uuid("some-random-helm-id").deploymentType(HELM).build();
    helmService = updateAndGetService(helmService, helmService);
    assertThat(helmService.getHelmVersion()).isEqualTo(V2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldNotUpdateHelmVersionForNonHelmService() {
    Service helmService = Service.builder().appId(APP_ID).uuid("some-random-helm-id").deploymentType(SSH).build();
    helmService = updateAndGetService(helmService, helmService);
    assertThat(helmService.getHelmVersion()).isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldBeNoopIfHelmVersionSet() {
    Service helm2Service = Service.builder().helmVersion(V2).deploymentType(HELM).build();
    Service helm3Service = Service.builder().helmVersion(V3).deploymentType(HELM).build();
    serviceResourceService.checkAndSetHelmVersion(helm2Service);
    serviceResourceService.checkAndSetHelmVersion(helm3Service);
    assertThat(helm2Service.getHelmVersion()).isEqualTo(V2);
    assertThat(helm2Service.getDeploymentType()).isEqualTo(HELM);
    assertThat(helm3Service.getHelmVersion()).isEqualTo(V3);
    assertThat(helm3Service.getDeploymentType()).isEqualTo(HELM);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldNotGenerateUpdateOperationIfHelmVersionSet() {
    Service oldHelm2Service = Service.builder().helmVersion(V2).deploymentType(HELM).build();
    Service newHelm2Service = Service.builder().helmVersion(V2).deploymentType(HELM).build();
    Service updatedService = updateAndGetService(oldHelm2Service, newHelm2Service);
    assertThat(updatedService).isEqualToIgnoringGivenFields(newHelm2Service, ServiceKeys.version, "lastUpdatedAt");

    Service oldHelm3Service = Service.builder().helmVersion(V3).deploymentType(HELM).build();
    Service newHelm3Service = Service.builder().helmVersion(V3).deploymentType(HELM).build();
    updatedService = updateAndGetService(oldHelm3Service, newHelm3Service);
    assertThat(updatedService).isEqualToIgnoringGivenFields(newHelm3Service, ServiceKeys.version, "lastUpdatedAt");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void shouldSwitchHelmVersions() {
    Service oldHelm2Service = Service.builder().helmVersion(V2).deploymentType(HELM).build();
    Service newHelm3Service = Service.builder().helmVersion(V3).deploymentType(HELM).build();
    Service updatedService = updateAndGetService(oldHelm2Service, newHelm3Service);
    assertThat(updatedService)
        .isEqualToIgnoringGivenFields(newHelm3Service, ServiceKeys.version, "lastUpdatedAt", ServiceKeys.helmVersion);
    assertThat(updatedService.getHelmVersion()).isEqualTo(V3);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmVersionIfNotPresent() {
    Service service = Service.builder().appId(APP_ID).uuid("1972510").deploymentType(HELM).build();
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmVersionWithDefault(APP_ID, service.getUuid())).isEqualTo(V2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmVersionIfPresent() {
    Service service = Service.builder().appId(APP_ID).uuid("017071").helmVersion(V2).build();
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmVersionWithDefault(APP_ID, service.getUuid())).isEqualTo(V2);

    service.setHelmVersion(V3);
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmVersionWithDefault(APP_ID, service.getUuid())).isEqualTo(V3);
  }

  private Service updateAndGetService(Service oldService, Service newService) {
    wingsPersistence.save(newService);
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);
    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);
    wingsPersistence.update(newService, updateOperations);
    newService = wingsPersistence.get(Service.class, newService.getUuid());
    return newService;
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testUpdateOperationsForHelmVersion() {
    opsMapShouldBeUpdatedWhenVersionPresent();
    opsMapShouldBeUpdatedWhenVersionNotPresentInOldAndNewService();
    opsMapShouldNotBeUpdatedWhenVersionNotPresentInNewService();
    opsMapShouldNotBeUpdatedWhenNonHelmK8sService();
  }

  private void opsMapShouldNotBeUpdatedWhenNonHelmK8sService() {
    Service newService = Service.builder().appId(APP_ID).deploymentType(SSH).build();
    Service oldService = Service.builder().appId(APP_ID).deploymentType(SSH).build();
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);

    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);

    Map<String, Object> ops =
        ReflectionUtils.getFieldValues(updateOperations, new HashSet<>(Collections.singletonList("ops")));
    Map opsMap = (Map) ops.get("ops");
    assertThat(opsMap.isEmpty()).isTrue();
  }

  private void opsMapShouldNotBeUpdatedWhenVersionNotPresentInNewService() {
    Service newService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    Service oldService = Service.builder().appId(APP_ID).deploymentType(HELM).helmVersion(V2).build();
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);

    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);

    Map<String, Object> ops =
        ReflectionUtils.getFieldValues(updateOperations, new HashSet<>(Collections.singletonList("ops")));
    Map opsMap = (Map) ops.get("ops");
    assertThat(opsMap.isEmpty()).isTrue();
  }

  private void opsMapShouldBeUpdatedWhenVersionNotPresentInOldAndNewService() {
    Service newService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    Service oldService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);

    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);

    Map<String, Object> ops =
        ReflectionUtils.getFieldValues(updateOperations, new HashSet<>(Collections.singletonList("ops")));
    Object value = ((Map) ((Map) ops.get("ops")).get("$set")).get(ServiceKeys.helmVersion);
    assertThat(value).isEqualTo(V2.name());
  }

  private void opsMapShouldBeUpdatedWhenVersionPresent() {
    Service newService = Service.builder().appId(APP_ID).deploymentType(HELM).helmVersion(V2).build();
    Service oldService = Service.builder().appId(APP_ID).deploymentType(HELM).helmVersion(V3).build();
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);

    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);

    Map<String, Object> ops =
        ReflectionUtils.getFieldValues(updateOperations, new HashSet<>(Collections.singletonList("ops")));
    Object value = ((Map) ((Map) ops.get("ops")).get("$set")).get(ServiceKeys.helmVersion);
    assertThat(value).isEqualTo(V2.name());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetHelmVersionWithDefault() {
    shouldGetHelmVersionWhenPresent();
    shouldReturnDefaultForHelmService();
    shouldReturnDefaultForK8sService();
  }

  private void shouldReturnDefaultForK8sService() {
    Service service = Service.builder().deploymentType(KUBERNETES).build();
    doReturn(service).when(spyServiceResourceService).get(APP_ID, SERVICE_ID);

    HelmVersion helmVersion = spyServiceResourceService.getHelmVersionWithDefault(APP_ID, SERVICE_ID);

    assertThat(helmVersion).isEqualTo(V3);
  }

  private void shouldReturnDefaultForHelmService() {
    Service service = Service.builder().deploymentType(HELM).build();
    doReturn(service).when(spyServiceResourceService).get(APP_ID, SERVICE_ID);

    HelmVersion helmVersion = spyServiceResourceService.getHelmVersionWithDefault(APP_ID, SERVICE_ID);

    assertThat(helmVersion).isEqualTo(V2);
  }

  private void shouldGetHelmVersionWhenPresent() {
    Service service = Service.builder().helmVersion(V2).build();
    doReturn(service).when(spyServiceResourceService).get(APP_ID, SERVICE_ID);

    HelmVersion helmVersion = spyServiceResourceService.getHelmVersionWithDefault(APP_ID, SERVICE_ID);

    assertThat(helmVersion).isEqualTo(V2);
  }
}
