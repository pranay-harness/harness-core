package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SearchFilter.Operator.NOT_EXISTS;
import static io.harness.beans.SearchFilter.Operator.OR;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V3;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.utils.ArtifactType;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class ServiceResourceServiceImplTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessTagService harnessTagService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private NotificationService notificationService;
  @Inject @InjectMocks private ServiceResourceServiceImpl serviceResourceService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @InjectMocks private ServiceResourceServiceImpl mockedServiceResourceService;
  @Mock private ResourceLookupService resourceLookupService;
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
  public void shouldNotUpdateHelmVersionIfNotPresent() {
    Service helmService = Service.builder().appId(APP_ID).uuid("some-random-helm-id").deploymentType(HELM).build();
    helmService = updateAndGetService(helmService, helmService);
    assertThat(helmService.getHelmVersion()).isNull();
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
    opsMapShouldNotBeUpdatedWhenVersionNotPresentInOldAndNewService();
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

  private void opsMapShouldNotBeUpdatedWhenVersionNotPresentInOldAndNewService() {
    Service newService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    Service oldService = Service.builder().appId(APP_ID).deploymentType(HELM).build();
    UpdateOperations<Service> updateOperations = wingsPersistence.createUpdateOperations(Service.class);

    serviceResourceService.updateOperationsForHelmVersion(oldService, newService, updateOperations);

    Map<String, Object> fieldValues =
        ReflectionUtils.getFieldValues(updateOperations, new HashSet<>(Collections.singletonList("ops")));
    Map opsMap = (Map) fieldValues.get("ops");
    assertThat(opsMap.isEmpty()).isTrue();
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

    assertThat(helmVersion).isEqualTo(V2);
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

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testListServiceWithInfraDefFilterError() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("infraDefinitionId", Collections.singletonList("infra1"));
    assertThatThrownBy(() -> mockedServiceResourceService.list(pageRequest, false, false, false, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("AppId is mandatory for infra-based filtering");
    assertThat(pageRequest.getFilters()).isEmpty();

    pageRequest.addFilter("appId", EQ, asList("app1", "app2").toArray());
    assertThatThrownBy(() -> mockedServiceResourceService.list(pageRequest, false, false, false, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("More than 1 appId not supported for listing services");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testListServiceWithInfraDefFilterNoScoping() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("infraDefinitionId", Collections.singletonList("infra1"));
    pageRequest.addFilter("appId", EQ, Collections.singletonList("app1").toArray());
    when(appService.getAccountIdByAppId("app1")).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.TEMPLATED_PIPELINES, ACCOUNT_ID)).thenReturn(true);
    InfrastructureDefinition sshInfraDef = InfrastructureDefinition.builder()
                                               .uuid("infra1")
                                               .name("ssh")
                                               .deploymentType(DeploymentType.SSH)
                                               .appId(APP_ID)
                                               .build();
    when(infrastructureDefinitionService.get("app1", "infra1")).thenReturn(sshInfraDef);

    PageResponse<Service> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.EMPTY_LIST);
    when(resourceLookupService.listWithTagFilters(pageRequest, null, EntityType.SERVICE, false))
        .thenReturn(pageResponse);

    mockedServiceResourceService.list(pageRequest, false, false, false, null);
    assertThat(pageRequest.getFilters()).isNotEmpty();
    assertThat(pageRequest.getFilters().size()).isEqualTo(2);
    assertThat(pageRequest.getFilters().get(1).getFieldName()).isEqualTo("deploymentType");
    assertThat(pageRequest.getFilters().get(1).getOp()).isEqualTo(OR);
    SearchFilter searchFilter1 = (SearchFilter) pageRequest.getFilters().get(1).getFieldValues()[0];
    SearchFilter searchFilter2 = (SearchFilter) pageRequest.getFilters().get(1).getFieldValues()[1];
    assertThat(searchFilter1.getOp()).isEqualTo(EQ);
    assertThat(searchFilter2.getOp()).isEqualTo(NOT_EXISTS);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testListServiceWithInfraDefFilterScoped() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("infraDefinitionId", Collections.singletonList("infra1"));
    pageRequest.addFilter("appId", EQ, Collections.singletonList("app1").toArray());
    when(appService.getAccountIdByAppId("app1")).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.TEMPLATED_PIPELINES, ACCOUNT_ID)).thenReturn(true);
    InfrastructureDefinition sshInfraDef = InfrastructureDefinition.builder()
                                               .uuid("infra1")
                                               .name("ssh")
                                               .scopedToServices(Collections.singletonList("s1"))
                                               .deploymentType(DeploymentType.SSH)
                                               .appId(APP_ID)
                                               .build();
    when(infrastructureDefinitionService.get("app1", "infra1")).thenReturn(sshInfraDef);

    PageResponse<Service> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.emptyList());
    when(resourceLookupService.listWithTagFilters(pageRequest, null, EntityType.SERVICE, false))
        .thenReturn(pageResponse);

    mockedServiceResourceService.list(pageRequest, false, false, false, null);
    assertThat(pageRequest.getFilters()).isNotEmpty();
    assertThat(pageRequest.getFilters().size()).isEqualTo(2);
    assertThat(pageRequest.getFilters().get(1).getFieldName()).isEqualTo("_id");
    assertThat(pageRequest.getFilters().get(1).getOp()).isEqualTo(IN);
    assertThat(pageRequest.getFilters().get(1).getFieldValues()).containsExactly("s1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testListServiceWithInfraDefFilterNoCommonScoping() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("infraDefinitionId", asList("infra1", "infra2", "infra3"));
    pageRequest.addFilter("appId", EQ, Collections.singletonList("app1").toArray());
    when(appService.getAccountIdByAppId("app1")).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.TEMPLATED_PIPELINES, ACCOUNT_ID)).thenReturn(true);
    InfrastructureDefinition sshInfraDef = InfrastructureDefinition.builder()
                                               .uuid("infra1")
                                               .name("ssh")
                                               .scopedToServices(Collections.singletonList("s1"))
                                               .deploymentType(DeploymentType.SSH)
                                               .appId(APP_ID)
                                               .build();
    when(infrastructureDefinitionService.get("app1", "infra1")).thenReturn(sshInfraDef);

    InfrastructureDefinition k8sInfraDef = InfrastructureDefinition.builder()
                                               .uuid("infra2")
                                               .name("k8s")
                                               .scopedToServices(Collections.singletonList("s2"))
                                               .deploymentType(DeploymentType.SSH)
                                               .appId(APP_ID)
                                               .build();
    when(infrastructureDefinitionService.get("app1", "infra2")).thenReturn(k8sInfraDef);

    InfrastructureDefinition noScopingInfraDef = InfrastructureDefinition.builder()
                                                     .uuid("infra2")
                                                     .name("k8s")
                                                     .deploymentType(DeploymentType.SSH)
                                                     .appId(APP_ID)
                                                     .build();
    when(infrastructureDefinitionService.get("app1", "infra3")).thenReturn(noScopingInfraDef);

    PageResponse<Service> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.emptyList());
    when(resourceLookupService.listWithTagFilters(pageRequest, null, EntityType.SERVICE, false))
        .thenReturn(pageResponse);

    assertThatThrownBy(() -> mockedServiceResourceService.list(pageRequest, false, false, false, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("No common scoped Services for selected Infra Definitions: [ssh, k8s]");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testListServiceWithDeploymentTypeFilter() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("deploymentTypeFromMetadata", asList("SSH", "KUBERNETES"));
    pageRequest.addFilter("appId", EQ, Collections.singletonList("app1").toArray());
    when(appService.getAccountIdByAppId("app1")).thenReturn(ACCOUNT_ID);
    PageResponse<Service> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.emptyList());
    when(resourceLookupService.listWithTagFilters(pageRequest, null, EntityType.SERVICE, false))
        .thenReturn(pageResponse);

    mockedServiceResourceService.list(pageRequest, false, false, false, null);
    assertThat(pageRequest.getFilters()).isNotEmpty();
    assertThat(pageRequest.getFilters().size()).isEqualTo(2);
    assertThat(pageRequest.getFilters().get(1).getFieldName()).isEqualTo("deploymentType");
    assertThat(pageRequest.getFilters().get(1).getOp()).isEqualTo(NOT_EXISTS);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldSetServiceDeploymentTypeAndArtifactTypeTag() {
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_SERVICE)))
        .thenReturn(new WingsTestConstants.MockChecker(true, ActionType.CREATE_SERVICE));
    when(applicationManifestService.create(any()))
        .thenReturn(ApplicationManifest.builder().storeType(StoreType.Local).build());

    doNothing().when(auditServiceHelper).addEntityOperationIdentifierDataToAuditContext(any());
    doNothing().when(notificationService).sendNotificationAsync(any());
    Service helmService = Service.builder()
                              .name(SERVICE_NAME)
                              .accountId(ACCOUNT_ID)
                              .appId(APP_ID)
                              .deploymentType(HELM)
                              .artifactType(ArtifactType.DOCKER)
                              .build();
    Service savedService = serviceResourceService.save(helmService);
    List<HarnessTagLink> tagLinksWithEntityId =
        harnessTagService.getTagLinksWithEntityId(ACCOUNT_ID, savedService.getUuid());
    assertThat(tagLinksWithEntityId).hasSize(2);
    assertTrue(tagLinksWithEntityId.stream().anyMatch(
        tagLink -> tagLink.getKey().equals("deploymentType") && tagLink.getValue().equals("HELM")));
    assertTrue(tagLinksWithEntityId.stream().anyMatch(
        tagLink -> tagLink.getKey().equals("artifactType") && tagLink.getValue().equals("DOCKER")));
  }
}
