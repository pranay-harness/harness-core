package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.infrastructure.ApplicationHostUsage.Builder.anApplicationHostUsage;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.beans.infrastructure.Infrastructure.Builder.anInfrastructure;
import static software.wings.beans.infrastructure.Infrastructure.InfrastructureType.STATIC;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.ApplicationHostUsage;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TagService;
import software.wings.utils.HostCsvFileHelper;
import software.wings.utils.WingsTestConstants;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 6/7/16.
 */
public class HostServiceTest extends WingsBaseTest {
  @Mock private HostCsvFileHelper csvFileHelper;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private WingsPersistence wingsPersistence;
  @Mock private InfrastructureService infrastructureService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SettingsService settingsService;
  @Mock private TagService tagService;
  @Mock private EnvironmentService environmentService;
  @Mock private NotificationService notificationService;
  @Mock private AppService appService;

  @Inject @InjectMocks private HostService hostService;

  @Mock private Query<ApplicationHost> applicationHostQuery;
  @Mock private FieldEnd applicationHostQueryEnd;
  @Mock private Query<Host> hostQuery;
  @Mock private FieldEnd hostQueryEnd;
  @Mock private UpdateOperations<ApplicationHost> updateOperations;
  @Mock private AggregationPipeline aggregationPipeline;

  private SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_ID)
          .withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build())
          .build();
  private HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host.Builder hostBuilder = aHost()
                                         .withAppId(APP_ID)
                                         .withInfraId(INFRA_ID)
                                         .withHostName(HOST_NAME)
                                         .withHostConnAttr(HOST_CONN_ATTR_PWD)
                                         .withHostConnectionCredential(CREDENTIAL);
  private ApplicationHost.Builder appHostBuilder = ApplicationHost.Builder.anApplicationHost()
                                                       .withAppId(APP_ID)
                                                       .withEnvId(ENV_ID)
                                                       .withInfraId(INFRA_ID)
                                                       .withHostName(HOST_NAME)
                                                       .withHost(hostBuilder.build());

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(infrastructureService.getInfraByEnvId(APP_ID, ENV_ID))
        .thenReturn(anInfrastructure().withType(STATIC).withUuid(INFRA_ID).build());

    when(wingsPersistence.createUpdateOperations(ApplicationHost.class)).thenReturn(updateOperations);

    when(wingsPersistence.createQuery(ApplicationHost.class)).thenReturn(applicationHostQuery);
    when(applicationHostQuery.field(anyString())).thenReturn(applicationHostQueryEnd);
    when(applicationHostQueryEnd.equal(anyObject())).thenReturn(applicationHostQuery);
    when(applicationHostQueryEnd.hasAnyOf(anyCollection())).thenReturn(applicationHostQuery);

    when(wingsPersistence.createQuery(Host.class)).thenReturn(hostQuery);
    when(hostQuery.field(anyString())).thenReturn(hostQueryEnd);
    when(hostQueryEnd.equal(anyObject())).thenReturn(hostQuery);
  }

  /**
   * Should list hosts.
   */
  @Test
  public void shouldListHosts() {
    PageResponse<ApplicationHost> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(appHostBuilder.but().build()));
    pageResponse.setTotal(1);
    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(SearchFilter.Builder.aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .build())
                                  .build();
    when(wingsPersistence.query(ApplicationHost.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ApplicationHost> hosts = hostService.list(pageRequest);
    assertThat(hosts).isNotNull();
    assertThat(hosts.getResponse().get(0)).isInstanceOf(ApplicationHost.class);
  }

  /**
   * Should get host.
   */
  @Test
  public void shouldGetHost() {
    ApplicationHost host = appHostBuilder.build();
    when(applicationHostQuery.get()).thenReturn(host);
    ApplicationHost savedHost = hostService.get(APP_ID, ENV_ID, HOST_ID);
    verify(applicationHostQuery).field("appId");
    verify(applicationHostQueryEnd).equal(APP_ID);
    verify(applicationHostQuery).field("envId");
    verify(applicationHostQueryEnd).equal(ENV_ID);
    verify(applicationHostQuery).field(ID_KEY);
    verify(applicationHostQueryEnd).equal(HOST_ID);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(ApplicationHost.class);
  }

  /**
   * Should update host.
   */
  @Test
  public void shouldUpdateHost() {
    Host host = hostBuilder.withUuid(HOST_ID).build();
    when(applicationHostQuery.get()).thenReturn(appHostBuilder.withHost(host).build());
    when(tagService.getDefaultTagForUntaggedHosts(APP_ID, ENV_ID))
        .thenReturn(
            aTag().withUuid(TAG_ID).withAppId(APP_ID).withEnvId(ENV_ID).withTagType(TagType.UNTAGGED_HOST).build());
    ApplicationHost savedHost = hostService.update(ENV_ID, host);
    verify(wingsPersistence).updateFields(Host.class, HOST_ID, ImmutableMap.of("hostConnAttr", HOST_CONN_ATTR_ID));
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(ApplicationHost.class);
  }

  /**
   * Should delete host.
   */
  @Test
  public void shouldDeleteHost() {
    ApplicationHost host = appHostBuilder.build();
    when(applicationHostQuery.get()).thenReturn(host);
    when(wingsPersistence.delete(any(Host.class))).thenReturn(true);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    hostService.delete(APP_ID, ENV_ID, HOST_ID);
    verify(applicationHostQuery).field("appId");
    verify(applicationHostQueryEnd).equal(APP_ID);
    verify(applicationHostQuery).field("envId");
    verify(applicationHostQueryEnd).equal(ENV_ID);
    verify(applicationHostQuery).field(ID_KEY);
    verify(applicationHostQueryEnd).equal(HOST_ID);
    verify(wingsPersistence).delete(host);
    verify(serviceTemplateService).deleteHostFromTemplates(host);
    verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  /**
   * Should delete by infra.
   */
  @Test
  public void shouldDeleteByInfra() {
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHostName(HOST_NAME)
                               .withHost(hostBuilder.withUuid(HOST_ID).build())
                               .build();
    when(applicationHostQuery.asList()).thenReturn(asList(host));
    when(wingsPersistence.delete(any(ApplicationHost.class))).thenReturn(true);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    hostService.deleteByInfra(INFRA_ID);

    verify(applicationHostQuery).asList();
    verify(applicationHostQuery).field("infraId");
    verify(applicationHostQueryEnd).equal(INFRA_ID);
    verify(wingsPersistence).delete(host);
    verify(serviceTemplateService).deleteHostFromTemplates(host);
  }

  /**
   * Should get hosts by tags.
   */
  @Test
  public void shouldGetHostsByTags() {
    List<Tag> tags = asList(aTag().withUuid(TAG_ID).build());
    when(applicationHostQuery.asList()).thenReturn(asList(appHostBuilder.withUuid(HOST_ID).build()));

    List<ApplicationHost> hosts = hostService.getHostsByTags(APP_ID, ENV_ID, tags);

    verify(applicationHostQuery).asList();
    verify(applicationHostQuery).field("appId");
    verify(applicationHostQueryEnd).equal(APP_ID);
    verify(applicationHostQuery).field("envId");
    verify(applicationHostQueryEnd).equal(ENV_ID);
    verify(applicationHostQuery).field("configTag");
    verify(applicationHostQueryEnd).hasAnyOf(tags.stream().map(Tag::getUuid).collect(Collectors.toList()));
    assertThat(hosts.get(0)).isInstanceOf(ApplicationHost.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  /**
   * Should get hosts by host ids.
   */
  @Test
  public void shouldGetHostsByHostIds() {
    when(applicationHostQuery.asList()).thenReturn(asList(appHostBuilder.withUuid(HOST_ID).build()));
    List<ApplicationHost> hosts = hostService.getHostsByHostIds(APP_ID, ENV_ID, asList(HOST_ID));

    verify(applicationHostQuery).asList();
    verify(applicationHostQuery).field("appId");
    verify(applicationHostQueryEnd).equal(APP_ID);
    verify(applicationHostQuery).field("envId");
    verify(applicationHostQueryEnd).equal(ENV_ID);
    verify(applicationHostQuery).field(ID_KEY);
    verify(applicationHostQueryEnd).hasAnyOf(asList(HOST_ID));
    assertThat(hosts.get(0)).isInstanceOf(ApplicationHost.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  /**
   * Should bulk save.
   */
  @Test
  public void shouldBulkSave() {
    Tag tag = aTag().withUuid(TAG_ID).build();
    ServiceTemplate serviceTemplate = aServiceTemplate().withUuid(TEMPLATE_ID).build();
    SettingAttribute hostConnAttr = aSettingAttribute()
                                        .withUuid(HOST_CONN_ATTR_ID)
                                        .withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build())
                                        .build();

    Host requestHost = aHost()
                           .withAppId(APP_ID)
                           .withInfraId(INFRA_ID)
                           .withHostNames(asList(HOST_NAME))
                           .withHostConnAttr(hostConnAttr)
                           .withConfigTag(tag)
                           .withServiceTemplates(asList(serviceTemplate))
                           .withServiceTemplates(asList(serviceTemplate))
                           .build();

    Host hostPreSave = aHost()
                           .withAppId(GLOBAL_APP_ID)
                           .withInfraId(INFRA_ID)
                           .withHostName(HOST_NAME)
                           .withHostConnAttr(hostConnAttr)
                           .build();
    Host hostPostSave = aHost()
                            .withUuid(HOST_ID)
                            .withAppId(APP_ID)
                            .withInfraId(INFRA_ID)
                            .withHostName(HOST_NAME)
                            .withHostConnAttr(hostConnAttr)
                            .build();
    ApplicationHost applicationHostPreSave = ApplicationHost.Builder.anApplicationHost()
                                                 .withAppId(APP_ID)
                                                 .withEnvId(ENV_ID)
                                                 .withInfraId(INFRA_ID)
                                                 .withHostName(HOST_NAME)
                                                 .withConfigTag(tag)
                                                 .withHost(hostPostSave)
                                                 .build();
    ApplicationHost applicationHostPostSave = ApplicationHost.Builder.anApplicationHost()
                                                  .withUuid(HOST_ID)
                                                  .withAppId(APP_ID)
                                                  .withEnvId(ENV_ID)
                                                  .withInfraId(INFRA_ID)
                                                  .withHostName(HOST_NAME)
                                                  .withConfigTag(tag)
                                                  .withHost(hostPostSave)
                                                  .build();

    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);
    when(tagService.get(APP_ID, ENV_ID, TAG_ID, true)).thenReturn(tag);
    when(wingsPersistence.saveAndGet(Host.class, hostPreSave)).thenReturn(hostPostSave);
    when(wingsPersistence.saveAndGet(ApplicationHost.class, applicationHostPreSave))
        .thenReturn(applicationHostPostSave);
    when(infrastructureService.get(INFRA_ID))
        .thenReturn(Infrastructure.Builder.anInfrastructure()
                        .withType(STATIC)
                        .withAppId(GLOBAL_APP_ID)
                        .withUuid(INFRA_ID)
                        .build());
    when(settingsService.get(GLOBAL_APP_ID, HOST_CONN_ATTR_ID)).thenReturn(hostConnAttr);

    hostService.bulkSave(INFRA_ID, ENV_ID, requestHost);

    verify(wingsPersistence).saveAndGet(Host.class, hostPreSave);
    verify(wingsPersistence).saveAndGet(ApplicationHost.class, applicationHostPreSave);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(tagService).get(APP_ID, ENV_ID, TAG_ID, true);
    verify(serviceTemplateService).addHosts(serviceTemplate, asList(applicationHostPostSave));
    verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  /**
   * Should remove tag from host.
   */
  @Test
  public void shouldRemoveTagFromHost() {
    Tag tag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Tag defaultTag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    ApplicationHost applicationHost = appHostBuilder.withUuid(HOST_ID)
                                          .withConfigTag(aTag().withUuid("TAG_ID_2").withRootTagId("TAG_ID_1").build())
                                          .build();
    when(tagService.getDefaultTagForUntaggedHosts(APP_ID, ENV_ID)).thenReturn(defaultTag);
    when(updateOperations.set("configTag", tag)).thenReturn(updateOperations);
    when(tagService.get(APP_ID, ENV_ID, "TAG_ID_2", false))
        .thenReturn(aTag().withUuid("TAG_ID_2").withRootTagId("TAG_ID_1").build());
    hostService.removeTagFromHost(applicationHost, tag);
    verify(wingsPersistence).createUpdateOperations(ApplicationHost.class);
    verify(updateOperations).set("configTag", defaultTag.getUuid());
  }

  /**
   * Should set tags.
   */
  @Test
  public void shouldSetTags() {
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(hostBuilder.withUuid(HOST_ID).build())
                               .build();

    Tag tag = aTag().withUuid(TAG_ID).build();
    when(updateOperations.set("configTag", tag.getUuid())).thenReturn(updateOperations);
    hostService.setTag(host, tag);
    verify(wingsPersistence).update(host, updateOperations);
    verify(wingsPersistence).createUpdateOperations(ApplicationHost.class);
    verify(updateOperations).set("configTag", tag.getUuid());
  }

  @Test
  public void shouldGetInfrastructureHostUsageByApplication() {
    List<Application> applications = asList(anApplication().withUuid("ID1").withName("NAME1").build(),
        anApplication().withUuid("ID2").withName("NAME2").build());
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(applications);
    when(appService.list(any(), eq(false), eq(0))).thenReturn(pageResponse);
    when(wingsPersistence.getDatastore().createAggregation(ApplicationHost.class)).thenReturn(aggregationPipeline);
    when(aggregationPipeline.match(applicationHostQuery)).thenReturn(aggregationPipeline);
    when(aggregationPipeline.group(anyString(), any())).thenReturn(aggregationPipeline);
    when(aggregationPipeline.aggregate(ApplicationHostUsage.class))
        .thenReturn(asList(anApplicationHostUsage().withAppId("ID1").withCount(1).build(),
            anApplicationHostUsage().withAppId("ID2").withCount(2).build())
                        .listIterator());

    List<ApplicationHostUsage> usageByApplication = hostService.getInfrastructureHostUsageByApplication(INFRA_ID);

    Assertions.assertThat(usageByApplication)
        .hasSize(2)
        .containsExactlyInAnyOrder(anApplicationHostUsage().withAppId("ID1").withAppName("NAME1").withCount(1).build(),
            anApplicationHostUsage().withAppId("ID2").withAppName("NAME2").withCount(2).build());
  }
}
