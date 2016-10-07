package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_TYPE;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.RELEASE_ID;
import static software.wings.utils.WingsTestConstants.RELEASE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceInstanceServiceImpl;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ExecutionStatus;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 5/26/16.
 */
public class ServiceInstanceServiceTest extends WingsBaseTest {
  private final ServiceTemplate serviceTemplate =
      aServiceTemplate().withUuid(TEMPLATE_ID).withService(aService().withUuid(SERVICE_ID).build()).build();
  private final Host host = aHost().withUuid(HOST_ID).build();
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query<ServiceInstance> query;
  @Mock private UpdateOperations<ServiceInstance> updateOperations;
  @Mock private FieldEnd end;
  @InjectMocks @Inject private ServiceInstanceService serviceInstanceService;
  @Spy @InjectMocks private ServiceInstanceService spyInstanceService = new ServiceInstanceServiceImpl();
  private ServiceInstance.Builder builder =
      aServiceInstance()
          .withAppId(APP_ID)
          .withEnvId(ENV_ID)
          .withHost(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build())
          .withServiceTemplate(serviceTemplate)
          .withReleaseId(RELEASE_NAME)
          .withReleaseId(RELEASE_ID)
          .withArtifactId(ARTIFACT_ID)
          .withArtifactName(ARTIFACT_NAME);

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(ServiceInstance.class)).thenReturn(query);
    when(query.field(anyString())).thenReturn(end);
    when(end.equal(anyObject())).thenReturn(query);
    when(end.hasAnyOf(anyCollection())).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(ServiceInstance.class)).thenReturn(updateOperations);
    when(updateOperations.set(anyString(), anyObject())).thenReturn(updateOperations);
  }

  /**
   * Should list service instances.
   */
  @Test
  public void shouldListServiceInstances() {
    PageResponse<ServiceInstance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(builder.build()));
    pageResponse.setTotal(1);
    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(SearchFilter.Builder.aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .build())
                                  .build();
    when(wingsPersistence.query(ServiceInstance.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceInstance> serviceInstances = serviceInstanceService.list(pageRequest);
    assertThat(serviceInstances).isNotNull();
    assertThat(serviceInstances.getResponse().get(0)).isInstanceOf(ServiceInstance.class);
  }

  /**
   * Should save service instance.
   */
  @Test
  public void shouldSaveServiceInstance() {
    ServiceInstance serviceInstance = builder.build();
    when(wingsPersistence.saveAndGet(eq(ServiceInstance.class), eq(serviceInstance))).thenReturn(serviceInstance);
    ServiceInstance savedServiceInstance = serviceInstanceService.save(serviceInstance);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  /**
   * Should update service instance.
   */
  @Test
  public void shouldUpdateActivity() {
    long createdAt = System.currentTimeMillis();
    Activity activity = Activity.Builder.anActivity()
                            .withAppId(APP_ID)
                            .withUuid(ACTIVITY_ID)
                            .withServiceInstanceId(SERVICE_INSTANCE_ID)
                            .withReleaseId(RELEASE_ID)
                            .withReleaseName(RELEASE_NAME)
                            .withArtifactId(ARTIFACT_ID)
                            .withArtifactName(ARTIFACT_NAME)
                            .withStatus(ExecutionStatus.SUCCESS)
                            .withCommandName(COMMAND_NAME)
                            .withCommandType(COMMAND_UNIT_TYPE)
                            .withCreatedAt(createdAt)
                            .build();
    serviceInstanceService.updateActivity(activity);

    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));

    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(SERVICE_INSTANCE_ID);

    verify(updateOperations).set("artifactId", ARTIFACT_ID);
    verify(updateOperations).set("artifactName", ARTIFACT_NAME);
    verify(updateOperations).set("releaseId", RELEASE_ID);
    verify(updateOperations).set("releaseName", RELEASE_NAME);
    verify(updateOperations).set("artifactDeployedOn", createdAt);
    verify(updateOperations).set("artifactDeploymentStatus", ExecutionStatus.SUCCESS);
    verify(updateOperations).set("artifactDeploymentActivityId", ACTIVITY_ID);
    verify(updateOperations).set("lastActivityId", ACTIVITY_ID);
    verify(updateOperations).set("lastActivityStatus", ExecutionStatus.SUCCESS);
    verify(updateOperations).set("commandName", COMMAND_NAME);
    verify(updateOperations).set("commandType", COMMAND_UNIT_TYPE);
    verify(updateOperations).set("lastActivityCreatedAt", createdAt);
  }

  /**
   * Should get service instance.
   */
  @Test
  public void shouldGetServiceInstance() {
    when(query.get()).thenReturn(builder.withUuid(SERVICE_INSTANCE_ID).build());
    ServiceInstance savedServiceInstance = serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
    verify(query).get();
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(SERVICE_INSTANCE_ID);
  }

  /**
   * Should delete service instance.
   */
  @Test
  public void shouldDeleteServiceInstance() {
    serviceInstanceService.delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(wingsPersistence).delete(query);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(SERVICE_INSTANCE_ID);
  }

  /**
   * Should delete by env.
   */
  @Test
  public void shouldDeleteByEnv() {
    when(query.asList()).thenReturn(asList(builder.withUuid(SERVICE_INSTANCE_ID).build()));
    doNothing().when(spyInstanceService).delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    spyInstanceService.deleteByEnv(APP_ID, ENV_ID);
    verify(spyInstanceService).delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).asList();
  }

  /**
   * Should delete by service template.
   */
  @Test
  public void shouldDeleteByServiceTemplate() {
    when(query.asList()).thenReturn(asList(builder.withUuid(SERVICE_INSTANCE_ID).build()));
    doNothing().when(spyInstanceService).delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    spyInstanceService.deleteByServiceTemplate(APP_ID, ENV_ID, TEMPLATE_ID);
    verify(spyInstanceService).delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("serviceTemplate");
    verify(end).equal(TEMPLATE_ID);
    verify(query).asList();
  }

  /**
   * Should update host instance mapping.
   */
  @Test
  public void shouldUpdateHostInstanceMapping() {
    Host newHost = aHost().withUuid("NEW_HOST_ID").build();
    Host deletedHost = aHost().withUuid("DELETED_HOST_ID").build();
    List<ApplicationHost> newHostList = asList(
        anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("NEW_HOST_ID").withHost(newHost).build());
    List<ApplicationHost> deletedHosts = asList(anApplicationHost()
                                                    .withAppId(APP_ID)
                                                    .withEnvId(ENV_ID)
                                                    .withUuid("DELETED_HOST_ID")
                                                    .withHost(deletedHost)
                                                    .build());
    ServiceTemplate serviceTemplate = aServiceTemplate()
                                          .withAppId(APP_ID)
                                          .withEnvId(ENV_ID)
                                          .withUuid(TEMPLATE_ID)
                                          .withService(aService().withUuid(SERVICE_ID).withName(SERVICE_NAME).build())
                                          .build();
    serviceInstanceService.updateInstanceMappings(serviceTemplate, newHostList, deletedHosts);
    verify(wingsPersistence).delete(isA(Query.class));
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("serviceTemplate");
    verify(end).equal(TEMPLATE_ID);
    verify(query).field("host");
    verify(end).hasAnyOf(asList("DELETED_HOST_ID"));
    verify(wingsPersistence)
        .save(aServiceInstance()
                  .withAppId(APP_ID)
                  .withEnvId(ENV_ID)
                  .withServiceTemplate(serviceTemplate)
                  .withHost(newHostList.get(0))
                  .build());
  }
}
