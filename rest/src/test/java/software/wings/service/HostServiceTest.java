package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.persistence.HQuery;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.utils.HostCsvFileHelper;
import software.wings.utils.WingsTestConstants;

import java.util.List;

/**
 * Created by anubhaw on 6/7/16.
 */
public class HostServiceTest extends WingsBaseTest {
  @Mock private HostCsvFileHelper csvFileHelper;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private WingsPersistence wingsPersistence;

  @Mock private EnvironmentService environmentService;
  @Mock private NotificationService notificationService;
  @Mock private ConfigService configService;
  @Mock private ServiceInstanceService serviceInstanceService;

  @Inject @InjectMocks private HostService hostService;

  @Mock private HQuery<Host> hostQuery;
  @Mock private FieldEnd hostQueryEnd;
  @Mock private UpdateOperations<Host> updateOperations;

  @Mock private JobScheduler jobScheduler;

  private SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_ID)
          .withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build())
          .build();
  private HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host.Builder hostBuilder =
      aHost().withAppId(APP_ID).withEnvId(ENV_ID).withHostName(HOST_NAME).withHostConnAttr(
          HOST_CONN_ATTR_PWD.getUuid());

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createUpdateOperations(Host.class)).thenReturn(updateOperations);

    when(wingsPersistence.createQuery(Host.class)).thenReturn(hostQuery);
    when(hostQuery.filter(any(), any())).thenReturn(hostQuery);

    when(hostQuery.field(anyString())).thenReturn(hostQueryEnd);
    when(hostQueryEnd.hasAnyOf(anyCollection())).thenReturn(hostQuery);

    when(wingsPersistence.createQuery(Host.class)).thenReturn(hostQuery);
  }

  /**
   * Should list hosts.
   */
  @Test
  public void shouldListHosts() {
    PageResponse<Host> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(hostBuilder.but().build()));
    pageResponse.setTotal(1l);
    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter("appId", EQ, APP_ID)
                                  .addFilter("envId", EQ, ENV_ID)
                                  .build();
    when(wingsPersistence.query(Host.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Host> hosts = hostService.list(pageRequest);
    assertThat(hosts).isNotNull();
    assertThat(hosts.getResponse().get(0)).isInstanceOf(Host.class);
  }

  /**
   * Should get host.
   */
  @Test
  public void shouldGetHost() {
    Host host = hostBuilder.build();
    when(hostQuery.get()).thenReturn(host);
    Host savedHost = hostService.get(APP_ID, ENV_ID, HOST_ID);
    verify(hostQuery).filter("appId", APP_ID);
    verify(hostQuery).filter("envId", ENV_ID);
    verify(hostQuery).filter(ID_KEY, HOST_ID);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  /**
   * Should update host.
   */
  @Test
  public void shouldUpdateHost() {
    Host host = hostBuilder.withUuid(HOST_ID).build();
    when(hostQuery.get()).thenReturn(hostBuilder.build());
    Host savedHost = hostService.update(ENV_ID, host);
    verify(wingsPersistence).updateFields(Host.class, HOST_ID, ImmutableMap.of("hostConnAttr", HOST_CONN_ATTR_ID));
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  /**
   * Should delete host.
   */
  @Test
  public void shouldDeleteHost() {
    Host host = hostBuilder.withAppId(APP_ID).withUuid(HOST_ID).build();
    when(hostQuery.get()).thenReturn(host);
    when(wingsPersistence.delete(any(Host.class))).thenReturn(true);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    hostService.delete(APP_ID, ENV_ID, HOST_ID);
    verify(hostQuery).filter("appId", APP_ID);
    verify(hostQuery).filter("envId", ENV_ID);
    verify(hostQuery).filter(ID_KEY, HOST_ID);
    verify(wingsPersistence).delete(host);
  }

  @Test
  public void shouldPruneDescendingObjects() {
    hostService.pruneDescendingEntities(APP_ID, HOST_ID);
    InOrder inOrder = inOrder(wingsPersistence, configService, serviceInstanceService);
    inOrder.verify(configService).pruneByHost(APP_ID, HOST_ID);
    inOrder.verify(serviceInstanceService).pruneByHost(APP_ID, HOST_ID);
  }

  /**
   * Should get hosts by host ids.
   */
  @Test
  public void shouldGetHostsByHostIds() {
    when(hostQuery.asList()).thenReturn(asList(hostBuilder.withUuid(HOST_ID).build()));
    List<Host> hosts = hostService.getHostsByHostIds(APP_ID, ENV_ID, asList(HOST_ID));

    verify(hostQuery).asList();
    verify(hostQuery).filter("appId", APP_ID);
    verify(hostQuery).filter("envId", ENV_ID);
    verify(hostQuery).field(ID_KEY);
    verify(hostQueryEnd).hasAnyOf(asList(HOST_ID));
    assertThat(hosts.get(0)).isInstanceOf(Host.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  /**
   * Should bulk save.
   */
  @Test
  @Ignore
  public void shouldBulkSave() {
    /*
        ServiceTemplate serviceTemplate = aServiceTemplate().withUuid(TEMPLATE_ID).build();
        SettingAttribute hostConnAttr =
            aSettingAttribute().withUuid(HOST_CONN_ATTR_ID).withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();

        Host requestHost =
       aHost().withAppId(APP_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttr.getUuid())
            .withServiceTemplates(asList(serviceTemplate)).withServiceTemplates(asList(serviceTemplate)).build();

        Host hostPreSave =
       aHost().withAppId(GLOBAL_APP_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttr).build();
        Host hostPostSave =
       aHost().withUuid(HOST_ID).withAppId(APP_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttr).build();
        Host applicationHostPreSave =
            Host.PageResponseBuilder.aHost().withAppId(APP_ID).withEnvId(ENV_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME)
                .withHost(hostPostSave).build();
        Host applicationHostPostSave =
            Host.PageResponseBuilder.aHost().withUuid(HOST_ID).withAppId(APP_ID).withEnvId(ENV_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME)
                .withHost(hostPostSave).build();

        when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
        when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);
        when(wingsPersistence.saveAndGet(Host.class, hostPreSave)).thenReturn(hostPostSave);
        when(wingsPersistence.saveAndGet(Host.class, applicationHostPreSave)).thenReturn(applicationHostPostSave);
        when(infrastructureService.get(INFRA_ID))
            .thenReturn(Infrastructure.PageResponseBuilder.anInfrastructure().withType(STATIC).withAppId(GLOBAL_APP_ID).withUuid(INFRA_ID).build());
        when(settingsService.get(GLOBAL_APP_ID, HOST_CONN_ATTR_ID)).thenReturn(hostConnAttr);

        hostService.bulkSave(INFRA_ID, ENV_ID, requestHost);

        verify(wingsPersistence).saveAndGet(Host.class, hostPreSave);
        verify(wingsPersistence).saveAndGet(Host.class, applicationHostPreSave);
        verify(notificationService).sendNotificationAsync(any(Notification.class));
        */
  }

  @Test
  @Ignore
  public void shouldGetInfrastructureHostUsageByApplication() {
    /*
        List<Application> applications =
            asList(anApplication().withUuid("ID1").withName("NAME1").build(),
       anApplication().withUuid("ID2").withName("NAME2").build()); PageResponse pageResponse = new PageResponse();
        pageResponse.setResponse(applications);
        when(appService.list(any(), eq(false), eq(0), eq(0))).thenReturn(pageResponse);
        when(wingsPersistence.getDatastore().createAggregation(Host.class)).thenReturn(aggregationPipeline);
        when(aggregationPipeline.match(hostQuery)).thenReturn(aggregationPipeline);
        when(aggregationPipeline.group(anyString(), any())).thenReturn(aggregationPipeline);
        when(aggregationPipeline.aggregate(HostUsage.class)).thenReturn(
            asList(HostUsage.PageResponseBuilder.aHostUsage().withAppId("ID1").withCount(1).build(),
       aHostUsage().withAppId("ID2").withCount(2).build()).listIterator());

        List<HostUsage> usageByApplication = hostService.getInfrastructureHostUsageByApplication(INFRA_ID);

        Assertions.assertThat(usageByApplication).hasSize(2)
            .containsExactlyInAnyOrder(aHostUsage().withAppId("ID1").withAppName("NAME1").withCount(1).build(),
                aHostUsage().withAppId("ID2").withAppName("NAME2").withCount(2).build());
                */
  }
}
