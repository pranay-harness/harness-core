package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Type.TEXT;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TARGET_SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Notification;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.stats.CloneMetadata;
import software.wings.common.Constants;
import software.wings.dl.HQuery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 6/28/16.
 */
public class EnvironmentServiceTest extends WingsBaseTest {
  @Mock private Application application;
  @Mock private ActivityService activityService;
  @Mock private AppService appService;
  @Mock private ConfigService configService;
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private NotificationService notificationService;
  @Mock private PipelineService pipelineService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private ServiceVariableService serviceVariableService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;
  @Mock private YamlChangeSetHelper yamlChangeSetHelper;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Inject @InjectMocks private EnvironmentService environmentService;

  @Spy @InjectMocks private EnvironmentService spyEnvService = new EnvironmentServiceImpl();

  @Mock private JobScheduler jobScheduler;
  @Mock private HQuery<Environment> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<Environment> updateOperations;

  @Captor private ArgumentCaptor<Environment> environmentArgumentCaptor;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Environment.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(Environment.class)).thenReturn(updateOperations);
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
    when(updateOperations.unset(any())).thenReturn(updateOperations);
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  /**
   * Should list environments.
   */
  @Test
  public void shouldListEnvironments() {
    Environment environment = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build();
    PageRequest<Environment> envPageRequest = new PageRequest<>();
    PageResponse<Environment> envPageResponse = new PageResponse<>();
    envPageResponse.setResponse(asList(environment));
    when(wingsPersistence.query(Environment.class, envPageRequest)).thenReturn(envPageResponse);

    ServiceTemplate serviceTemplate = aServiceTemplate().build();
    PageRequest<ServiceTemplate> serviceTemplatePageRequest = new PageRequest<>();
    serviceTemplatePageRequest.addFilter("appId", Operator.EQ, environment.getAppId());
    serviceTemplatePageRequest.addFilter("envId", EQ, environment.getUuid());
    PageResponse<ServiceTemplate> serviceTemplatePageResponse = new PageResponse<>();
    serviceTemplatePageResponse.setResponse(asList(serviceTemplate));
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(serviceTemplatePageResponse);

    PageResponse<Environment> environments = environmentService.list(envPageRequest, true);

    assertThat(environments).containsAll(asList(environment));
    assertThat(environments.get(0).getServiceTemplates()).containsAll(asList(serviceTemplate));
    verify(serviceTemplateService).list(serviceTemplatePageRequest, false, false);
  }

  /**
   * Should get environment.
   */
  @Test
  public void shouldGetEnvironment() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(serviceTemplateService.list(any(PageRequest.class), eq(false), eq(false))).thenReturn(new PageResponse<>());
    environmentService.get(APP_ID, ENV_ID, true);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  @Test
  public void shouldGetEnvironmentOnly() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    Environment environment = environmentService.get(APP_ID, ENV_ID);
    assertThat(environment).isNotNull();
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }
  @Test
  public void shouldReturnTrueForExistingEnvironmentInExistApi() {
    when(query.getKey()).thenReturn(new Key<>(Environment.class, "environments", ENV_ID));
    assertThat(environmentService.exist(APP_ID, ENV_ID)).isTrue();
    verify(query).filter(ID_KEY, ENV_ID);
    verify(query).filter("appId", APP_ID);
  }

  /**
   * Should save environment.
   */
  @Test
  public void shouldSaveEnvironment() {
    Environment environment =
        anEnvironment().withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    Environment savedEnvironment =
        anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    when(wingsPersistence.saveAndGet(Environment.class, environment)).thenReturn(savedEnvironment);

    environmentService.save(environment);
    verify(wingsPersistence).saveAndGet(Environment.class, environment);
    verify(serviceTemplateService).createDefaultTemplatesByEnv(savedEnvironment);
    verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  /**
   * Should clone environment.
   */
  @Test
  public void shouldCloneEnvironment() {
    Environment environment =
        anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    Environment clonedEnvironment = environment.cloneInternal();
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID)).thenReturn(environment);
    when(wingsPersistence.saveAndGet(any(), any(Environment.class))).thenReturn(clonedEnvironment);

    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", SearchFilter.Operator.EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());

    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withUuid(TEMPLATE_ID)
                                          .withDescription(TEMPLATE_DESCRIPTION)
                                          .withServiceId(SERVICE_ID)
                                          .withEnvId(Base.GLOBAL_ENV_ID)
                                          .withInfrastructureMappings(asList(physicalInfrastructureMapping))
                                          .build();

    ServiceTemplate clonedServiceTemplate = serviceTemplate.cloneInternal();
    PageResponse<ServiceTemplate> pageResponse = aPageResponse().withResponse(asList(serviceTemplate)).build();
    when(serviceTemplateService.list(pageRequest, false, false)).thenReturn(pageResponse);
    when(serviceTemplateService.save(any(ServiceTemplate.class))).thenReturn(clonedServiceTemplate);
    when(serviceTemplateService.get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true))
        .thenReturn(serviceTemplate);

    CloneMetadata cloneMetadata = CloneMetadata.builder().environment(environment).build();
    environmentService.cloneEnvironment(APP_ID, ENV_ID, cloneMetadata);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Environment.class));
    verify(serviceTemplateService).list(pageRequest, false, false);
    verify(serviceTemplateService).save(any(ServiceTemplate.class));
    verify(serviceTemplateService).get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true);
  }

  /**
   * Should clone environment.
   */
  @Test
  public void shouldCloneEnvironmentAcrossApp() {
    when(appService.get(APP_ID)).thenReturn(application);
    Environment environment =
        anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    Environment clonedEnvironment = environment.cloneInternal();
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID)).thenReturn(environment);
    when(wingsPersistence.saveAndGet(any(), any(Environment.class))).thenReturn(clonedEnvironment);

    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", SearchFilter.Operator.EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());

    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withUuid(TEMPLATE_ID)
                                          .withDescription(TEMPLATE_DESCRIPTION)
                                          .withServiceId(SERVICE_ID)
                                          .withEnvId(Base.GLOBAL_ENV_ID)
                                          .withInfrastructureMappings(asList(physicalInfrastructureMapping))
                                          .build();

    ServiceTemplate clonedServiceTemplate = serviceTemplate.cloneInternal();
    PageResponse<ServiceTemplate> pageResponse = aPageResponse().withResponse(asList(serviceTemplate)).build();
    when(serviceTemplateService.list(pageRequest, false, false)).thenReturn(pageResponse);
    when(serviceTemplateService.save(any(ServiceTemplate.class))).thenReturn(clonedServiceTemplate);
    when(serviceTemplateService.get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true))
        .thenReturn(serviceTemplate);

    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(WAR).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(WAR).build());
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());

    CloneMetadata cloneMetadata = CloneMetadata.builder()
                                      .environment(environment)
                                      .serviceMapping(ImmutableMap.of(SERVICE_ID, TARGET_SERVICE_ID))
                                      .targetAppId(TARGET_APP_ID)
                                      .build();

    environmentService.cloneEnvironment(APP_ID, ENV_ID, cloneMetadata);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Environment.class));
    verify(serviceTemplateService).list(pageRequest, false, false);
    verify(serviceTemplateService).save(any(ServiceTemplate.class));
    verify(serviceTemplateService).get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true);
  }

  /**
   * Should update environment.
   */
  @Test
  public void shouldUpdateEnvironment() {
    Environment savedEnv = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName("PROD").build();
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID)).thenReturn(savedEnv);
    Environment environment = anEnvironment()
                                  .withAppId(APP_ID)
                                  .withUuid(ENV_ID)
                                  .withName(ENV_NAME)
                                  .withEnvironmentType(PROD)
                                  .withDescription(ENV_DESCRIPTION)
                                  .build();
    environmentService.update(environment);
    verify(wingsPersistence).update(savedEnv, updateOperations);
    verify(wingsPersistence, times(2)).get(Environment.class, APP_ID, ENV_ID);
  }

  /**
   * Should delete environment.
   */
  @Test
  public void shouldDeleteEnvironment() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName("PROD").build());
    when(wingsPersistence.delete(any(Environment.class))).thenReturn(true);
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(aPageResponse().build());
    environmentService.delete(APP_ID, ENV_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceTemplateService, notificationService);
    inOrder.verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
    inOrder.verify(wingsPersistence).delete(any(Environment.class));
    inOrder.verify(notificationService).sendNotificationAsync(any());
  }

  // We are not throwing an exception anymore and this will be ignored for now
  @Test
  public void shouldThrowExceptionOnReferencedEnvironmentDelete() {
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName("PROD").build());
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(pipelineService.listPipelines(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(Pipeline.builder().name("PIPELINE_NAME").build())).build());
    List<String> pipelineNames = new ArrayList<>();
    pipelineNames.add("PIPELINE_NAME");
    when(pipelineService.isEnvironmentReferenced(APP_ID, ENV_ID)).thenReturn(pipelineNames);
    assertThatThrownBy(() -> environmentService.delete(APP_ID, ENV_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());
  }

  @Test
  public void shouldPruneByApplication() {
    when(query.asList())
        .thenReturn(asList(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName("PROD").build()));
    when(wingsPersistence.delete(any(Environment.class))).thenReturn(true);
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(aPageResponse().build());
    environmentService.pruneByApplication(APP_ID);
    InOrder inOrder =
        inOrder(wingsPersistence, activityService, serviceTemplateService, notificationService, workflowService);
    inOrder.verify(wingsPersistence).delete(any(Environment.class));
    inOrder.verify(activityService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(serviceTemplateService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(workflowService).pruneByEnvironment(APP_ID, ENV_ID);
  }

  @Test
  public void shouldPruneDescendingObjects() {
    environmentService.pruneDescendingEntities(APP_ID, ENV_ID);

    InOrder inOrder =
        inOrder(wingsPersistence, activityService, serviceTemplateService, notificationService, workflowService);
    inOrder.verify(activityService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(serviceTemplateService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(workflowService).pruneByEnvironment(APP_ID, ENV_ID);
  }

  @Test
  public void shouldPruneDescendingObjectsSomeFailed() {
    doThrow(new WingsException("Forced exception")).when(serviceTemplateService).pruneByEnvironment(APP_ID, ENV_ID);

    assertThatThrownBy(() -> environmentService.pruneDescendingEntities(APP_ID, ENV_ID));

    InOrder inOrder =
        inOrder(wingsPersistence, activityService, serviceTemplateService, notificationService, workflowService);
    inOrder.verify(activityService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(serviceTemplateService).pruneByEnvironment(APP_ID, ENV_ID);
    inOrder.verify(workflowService).pruneByEnvironment(APP_ID, ENV_ID);
  }

  /**
   * Should create default environments.
   */
  @Test
  public void shouldCreateDefaultEnvironments() {
    doReturn(anEnvironment().build()).when(spyEnvService).save(any(Environment.class));
    spyEnvService.createDefaultEnvironments(APP_ID);
    verify(spyEnvService, times(3)).save(environmentArgumentCaptor.capture());
    assertThat(environmentArgumentCaptor.getAllValues())
        .extracting(Environment::getName)
        .containsExactly(Constants.DEV_ENV, Constants.QA_ENV, Constants.PROD_ENV);
  }

  @Test
  public void shouldGetServicesWithOverridesEmpty() {
    Environment environment =
        anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID)).thenReturn(environment);

    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", SearchFilter.Operator.EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());

    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withUuid(TEMPLATE_ID)
                                          .withDescription(TEMPLATE_DESCRIPTION)
                                          .withServiceId(SERVICE_ID)
                                          .withEnvId(Base.GLOBAL_ENV_ID)
                                          .withInfrastructureMappings(asList(physicalInfrastructureMapping))
                                          .build();

    PageResponse<ServiceTemplate> pageResponse = aPageResponse().withResponse(asList(serviceTemplate)).build();
    when(serviceTemplateService.list(pageRequest, false, false)).thenReturn(pageResponse);
    when(serviceTemplateService.get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true))
        .thenReturn(serviceTemplate);

    List<Service> services = environmentService.getServicesWithOverrides(APP_ID, ENV_ID);
    assertThat(services).isNotNull().size().isEqualTo(0);

    verify(serviceVariableService).getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, true);
    verify(configService)
        .getConfigFileByTemplate(environment.getAppId(), environment.getUuid(), serviceTemplate.getUuid());
  }

  @Test
  public void shouldGetServicesWithOverrides() {
    Environment environment =
        anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    when(wingsPersistence.get(Environment.class, APP_ID, ENV_ID)).thenReturn(environment);

    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", SearchFilter.Operator.EQ, environment.getAppId());
    pageRequest.addFilter("envId", EQ, environment.getUuid());

    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withUuid(TEMPLATE_ID)
                                          .withDescription(TEMPLATE_DESCRIPTION)
                                          .withServiceId(SERVICE_ID)
                                          .withEnvId(Base.GLOBAL_ENV_ID)
                                          .withInfrastructureMappings(asList(physicalInfrastructureMapping))
                                          .build();

    PageResponse<ServiceTemplate> pageResponse = aPageResponse().withResponse(asList(serviceTemplate)).build();
    when(serviceTemplateService.list(pageRequest, false, false)).thenReturn(pageResponse);
    when(serviceTemplateService.get(APP_ID, serviceTemplate.getEnvId(), serviceTemplate.getUuid(), true, true))
        .thenReturn(serviceTemplate);

    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .envId(ENV_ID)
                                          .entityType(EntityType.SERVICE_TEMPLATE)
                                          .entityId(TEMPLATE_ID)
                                          .templateId(TEMPLATE_ID)
                                          .name(SERVICE_VARIABLE_NAME)
                                          .type(TEXT)
                                          .value("8080".toCharArray())
                                          .build();
    serviceVariable.setAppId(APP_ID);
    serviceVariable.setUuid(SERVICE_VARIABLE_ID);

    PageResponse<ServiceVariable> serviceVariableResponse =
        aPageResponse().withResponse(asList(serviceVariable)).build();
    when(serviceVariableService.getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, true))
        .thenReturn(serviceVariableResponse);

    Service service = Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).appId(APP_ID).build();
    PageRequest<Service> servicePageRequest = aPageRequest()
                                                  .withLimit(PageRequest.UNLIMITED)
                                                  .addFilter("appId", EQ, environment.getAppId())
                                                  .addFilter("uuid", IN, asList(SERVICE_ID).toArray())
                                                  .addFieldsExcluded("appContainer")
                                                  .build();
    PageResponse<Service> servicesResponse = aPageResponse().withResponse(asList(service)).build();

    when(serviceResourceService.list(servicePageRequest, false, false)).thenReturn(servicesResponse);

    List<Service> services = environmentService.getServicesWithOverrides(APP_ID, ENV_ID);

    assertThat(services).isNotNull().size().isEqualTo(1);
    verify(serviceVariableService).getServiceVariablesByTemplate(APP_ID, ENV_ID, serviceTemplate, true);
    verify(configService, times(0))
        .getConfigFileByTemplate(environment.getAppId(), environment.getUuid(), serviceTemplate.getUuid());
  }
}
