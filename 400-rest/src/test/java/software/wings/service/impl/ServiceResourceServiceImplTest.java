package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SearchFilter.Operator.NOT_EXISTS;
import static io.harness.beans.SearchFilter.Operator.OR;
import static io.harness.k8s.model.HelmVersion.V2;
import static io.harness.k8s.model.HelmVersion.V3;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.CUSTOM;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.DELETE;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.FETCH;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.HISTORY;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.INSTALL;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.LIST;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.PULL;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.ROLLBACK;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.TEMPLATE;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.UNINSTALL;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.UPGRADE;
import static software.wings.beans.HelmCommandFlagConstants.HelmSubCommand.VERSION;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.k8s.model.HelmVersion;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.ArtifactType;
import software.wings.utils.WingsTestConstants.MockChecker;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.UpdateOperations;

public class ServiceResourceServiceImplTest extends WingsBaseTest {
  @Captor ArgumentCaptor<Command> commandCaptor;
  @Captor ArgumentCaptor<Boolean> booleanCaptor;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessTagService harnessTagService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;
  @Mock private LimitCheckerFactory limitCheckerFactory;
  @Mock private AuditServiceHelper auditServiceHelper;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private NotificationService notificationService;
  @Mock private CommandService commandService;
  @Inject @InjectMocks private ServiceResourceServiceImpl serviceResourceService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @InjectMocks private ServiceResourceServiceImpl mockedServiceResourceService;
  @InjectMocks @Inject private EntityVersionService entityVersionService;
  @Mock private ResourceLookupService resourceLookupService;
  @Mock private CustomDeploymentTypeService customDeploymentTypeService;
  @Mock private YamlPushService yamlPushService;
  private ServiceResourceServiceImpl spyServiceResourceService = spy(new ServiceResourceServiceImpl());

  @Before
  public void setUp() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_SERVICE)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_SERVICE));
  }

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
    assertThat(pageRequest.getFilters().stream().map(SearchFilter::getFieldName).collect(Collectors.toSet()))
        .doesNotContain(ServiceKeys.deploymentTypeTemplateId);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListServiceWithInfraDefFilterNoScopingCustomDeployment() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    PageRequest<Service> pageRequest = new PageRequest<>();
    pageRequest.setUriInfo(uriInfo);

    queryParams.put("infraDefinitionId", Collections.singletonList("infra1"));
    pageRequest.addFilter("appId", EQ, Collections.singletonList("app1").toArray());
    when(appService.getAccountIdByAppId("app1")).thenReturn(ACCOUNT_ID);
    InfrastructureDefinition customInfraDef = InfrastructureDefinition.builder()
                                                  .uuid("infra1")
                                                  .name("ssh")
                                                  .deploymentType(CUSTOM)
                                                  .appId(APP_ID)
                                                  .deploymentTypeTemplateId(TEMPLATE_ID)
                                                  .customDeploymentName("my-deployment")
                                                  .build();
    when(infrastructureDefinitionService.get("app1", "infra1")).thenReturn(customInfraDef);

    PageResponse<Service> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.EMPTY_LIST);
    when(resourceLookupService.listWithTagFilters(pageRequest, null, EntityType.SERVICE, false))
        .thenReturn(pageResponse);

    mockedServiceResourceService.list(pageRequest, false, false, false, null);
    assertThat(pageRequest.getFilters()).isNotEmpty();
    assertThat(pageRequest.getFilters().size()).isEqualTo(3);
    assertThat(pageRequest.getFilters().get(1).getFieldName()).isEqualTo("deploymentType");
    assertThat(pageRequest.getFilters().get(1).getOp()).isEqualTo(OR);
    SearchFilter searchFilter1 = (SearchFilter) pageRequest.getFilters().get(1).getFieldValues()[0];
    SearchFilter searchFilter2 = (SearchFilter) pageRequest.getFilters().get(1).getFieldValues()[1];
    assertThat(searchFilter1.getOp()).isEqualTo(EQ);
    assertThat(searchFilter2.getOp()).isEqualTo(NOT_EXISTS);
    assertThat(pageRequest.getFilters()
                   .stream()
                   .filter(f -> ServiceKeys.deploymentTypeTemplateId.equals(f.getFieldName()))
                   .collect(Collectors.toList()))
        .containsExactly(SearchFilter.builder()
                             .fieldName(ServiceKeys.deploymentTypeTemplateId)
                             .op(EQ)
                             .fieldValues(new Object[] {TEMPLATE_ID})
                             .build());
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
    assertThat(pageResponse.getFilters().stream().map(SearchFilter::getFieldName).collect(Collectors.toSet()))
        .doesNotContain(ServiceKeys.deploymentTypeTemplateId);
    assertThat(pageRequest.getFilters().stream().map(SearchFilter::getFieldName).collect(Collectors.toSet()))
        .doesNotContain(ServiceKeys.deploymentType);
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
    when(featureFlagService.isEnabled(FeatureName.HARNESS_TAGS, ACCOUNT_ID)).thenReturn(true);
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_SERVICE)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_SERVICE));
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

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateServiceWithHelmVersion() {
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.HARNESS_TAGS, ACCOUNT_ID)).thenReturn(true);
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_SERVICE)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_SERVICE));

    Service k8sService = Service.builder()
                             .name(SERVICE_NAME)
                             .accountId(ACCOUNT_ID)
                             .appId(APP_ID)
                             .deploymentType(KUBERNETES)
                             .artifactType(ArtifactType.DOCKER)
                             .description("Description")
                             .isK8sV2(true)
                             .build();
    Service service = serviceResourceService.save(k8sService);
    assertThat(service.getHelmVersion()).isEqualTo(HelmVersion.V2);
    assertThat(service.getDescription()).isEqualTo("Description");

    service.setHelmVersion(HelmVersion.V3);
    service.setDescription("UpdatedDescription");
    service = serviceResourceService.updateServiceWithHelmVersion(service);
    assertThat(service.getHelmVersion()).isEqualTo(HelmVersion.V3);
    assertThat(service.getDescription()).isEqualTo("Description");

    service.setHelmVersion(null);
    try {
      serviceResourceService.updateServiceWithHelmVersion(service);
      fail("Should not reach here");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Helm Version is not set");
    }

    service.setK8sV2(false);
    service = serviceResourceService.save(service);
    try {
      serviceResourceService.updateServiceWithHelmVersion(service);
      service.setHelmVersion(null);
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Setting helm version is supported only for kubernetes deployment type");
    }

    service.setDeploymentType(AWS_CODEDEPLOY);
    service.setHelmVersion(null);
    service = serviceResourceService.save(service);
    try {
      serviceResourceService.updateServiceWithHelmVersion(service);
      service.setHelmVersion(null);
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage()).isEqualTo("Setting helm version is supported only for kubernetes deployment type");
    }
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSaveCustomDeploymentTypeService() {
    Service service = Service.builder()
                          .name("custom-service")
                          .appId(APP_ID)
                          .deploymentType(CUSTOM)
                          .deploymentTypeTemplateId(TEMPLATE_ID)
                          .build();
    serviceResourceService.save(service);

    verify(customDeploymentTypeService, times(1)).putCustomDeploymentTypeNameIfApplicable(service);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListByDeploymentType() {
    wingsPersistence.save(Service.builder().uuid("1").appId(APP_ID).deploymentType(SSH).build());
    wingsPersistence.save(Service.builder().uuid("2").appId(APP_ID).deploymentType(KUBERNETES).build());
    wingsPersistence.save(Service.builder().uuid("3").appId(APP_ID).artifactType(ArtifactType.JAR).build());
    wingsPersistence.save(Service.builder().uuid("4").appId(APP_ID).artifactType(ArtifactType.AWS_CODEDEPLOY).build());
    wingsPersistence.save(
        Service.builder().uuid("5").appId(APP_ID).deploymentType(CUSTOM).deploymentTypeTemplateId(TEMPLATE_ID).build());
    assertThat(serviceResourceService.listByDeploymentType(APP_ID, SSH.name(), null)
                   .stream()
                   .map(Service::getUuid)
                   .collect(Collectors.toList()))
        .containsExactly("1", "3");

    assertThat(serviceResourceService.listByDeploymentType(APP_ID, CUSTOM.name(), TEMPLATE_ID)
                   .stream()
                   .map(Service::getUuid)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("3", "5");
    asList("1", "2", "3", "4", "5").forEach(id -> wingsPersistence.delete(Service.class, id));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateCustomDeploymentTypeService() {
    Service service = Service.builder()
                          .name("custom-service")
                          .appId(APP_ID)
                          .uuid(SERVICE_ID)
                          .deploymentType(CUSTOM)
                          .deploymentTypeTemplateId(TEMPLATE_ID)
                          .build();
    wingsPersistence.save(service);
    serviceResourceService.update(service);

    verify(customDeploymentTypeService, times(1)).putCustomDeploymentTypeNameIfApplicable(service);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testDeleteServiceWithServiceCommand() {
    final String UUID = RandomStringUtils.randomAlphanumeric(32);

    Command command = aCommand().withName("Install").withAccountId(ACCOUNT_ID).withOriginEntityId(UUID).build();
    command.setAppId(APP_ID);

    ServiceCommand serviceCommand = aServiceCommand()
                                        .withName("Install")
                                        .withServiceId(SERVICE_ID)
                                        .withAppId(APP_ID)
                                        .withAccountId(ACCOUNT_ID)
                                        .withUuid(UUID)
                                        .build();

    List<ServiceCommand> serviceCommands = new ArrayList<>();
    serviceCommands.add(serviceCommand);

    Service service = Service.builder()
                          .name("custom-service")
                          .appId(APP_ID)
                          .uuid(SERVICE_ID)
                          .deploymentType(CUSTOM)
                          .accountId(ACCOUNT_ID)
                          .deploymentTypeTemplateId(TEMPLATE_ID)
                          .serviceCommands(serviceCommands)
                          .build();

    wingsPersistence.save(command);
    wingsPersistence.save(serviceCommand);
    wingsPersistence.save(service);

    serviceResourceService.delete(APP_ID, SERVICE_ID);
    verify(appService, times(2)).getAccountIdByAppId(APP_ID);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSaveClonedCommandWithNewName() {
    ServiceCommand clonedServiceCommand = ServiceCommand.Builder.aServiceCommand()
                                              .withAccountId(ACCOUNT_ID)
                                              .withAppId(APP_ID)
                                              .withName("Name-clone")
                                              .withServiceId(SERVICE_ID)
                                              .withCommand(Command.Builder.aCommand()
                                                               .withCommandType(CommandType.OTHER)
                                                               .withName("Name")
                                                               .withAccountId(ACCOUNT_ID)
                                                               .build())
                                              .build();
    serviceResourceService.addServiceCommand(APP_ID, SERVICE_ID, clonedServiceCommand, false);
    verify(commandService).save(commandCaptor.capture(), booleanCaptor.capture());

    assertThat(commandCaptor.getValue().getName()).isEqualTo(clonedServiceCommand.getName());
    assertThat(booleanCaptor.getValue()).isFalse();
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetHelmCommandFlags() {
    Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).helmVersion(V2).deploymentType(HELM).build();
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmCommandFlags(V2, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, DELETE, UPGRADE, HISTORY);
    assertThat(serviceResourceService.getHelmCommandFlags(null, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, DELETE, UPGRADE, HISTORY);
    assertThat(serviceResourceService.getHelmCommandFlags(V3, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, UNINSTALL, UPGRADE, HISTORY);

    assertThat(serviceResourceService.getHelmCommandFlags(V2, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, DELETE, UPGRADE, HISTORY, FETCH);
    assertThat(serviceResourceService.getHelmCommandFlags(null, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, DELETE, UPGRADE, HISTORY, FETCH);
    assertThat(serviceResourceService.getHelmCommandFlags(V3, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(LIST, INSTALL, TEMPLATE, ROLLBACK, VERSION, UNINSTALL, UPGRADE, HISTORY, PULL);

    service.setDeploymentType(KUBERNETES);
    service.setHelmVersion(V3);
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmCommandFlags(V2, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(VERSION, TEMPLATE);
    assertThat(serviceResourceService.getHelmCommandFlags(null, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(VERSION, TEMPLATE);
    assertThat(serviceResourceService.getHelmCommandFlags(V3, APP_ID, SERVICE_ID, StoreType.HelmChartRepo))
        .containsExactlyInAnyOrder(VERSION, TEMPLATE);

    assertThat(serviceResourceService.getHelmCommandFlags(V2, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(FETCH, VERSION, TEMPLATE);
    assertThat(serviceResourceService.getHelmCommandFlags(null, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(PULL, VERSION, TEMPLATE);
    assertThat(serviceResourceService.getHelmCommandFlags(V3, APP_ID, SERVICE_ID, StoreType.HelmSourceRepo))
        .containsExactlyInAnyOrder(PULL, VERSION, TEMPLATE);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetHelmCommandFlagsForOldServices() {
    Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).build();
    wingsPersistence.save(service);
    assertThat(serviceResourceService.getHelmCommandFlags(null, APP_ID, SERVICE_ID, null)).isEmpty();
  }
}
