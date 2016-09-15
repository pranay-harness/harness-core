package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.beans.infrastructure.StaticInfrastructure.Builder.aStaticInfrastructure;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.ApplicationHost;
import software.wings.beans.Base;
import software.wings.beans.Host;
import software.wings.beans.Host.Builder;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.Notification;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
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
import javax.inject.Inject;

/**
 * Created by anubhaw on 6/7/16.
 */
public class HostServiceTest extends WingsBaseTest {
  @Mock private HostCsvFileHelper csvFileHelper;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private InfrastructureService infrastructureService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SettingsService settingsService;
  @Mock private TagService tagService;
  @Mock private EnvironmentService environmentService;
  @Mock private NotificationService notificationService;

  @Inject @InjectMocks private HostService hostService;

  @Mock private Query<ApplicationHost> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<ApplicationHost> updateOperations;

  private SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Builder builder = aHost()
                                .withAppId(APP_ID)
                                .withInfraId(INFRA_ID)
                                .withHostName(HOST_NAME)
                                .withHostConnAttr(HOST_CONN_ATTR_PWD)
                                .withHostConnectionCredential(CREDENTIAL);

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(infrastructureService.getInfraByEnvId(ENV_ID)).thenReturn(aStaticInfrastructure().withUuid(INFRA_ID).build());

    when(wingsPersistence.createUpdateOperations(ApplicationHost.class)).thenReturn(updateOperations);
    when(wingsPersistence.createQuery(ApplicationHost.class)).thenReturn(query);
    when(query.field(anyString())).thenReturn(end);
    when(end.equal(anyObject())).thenReturn(query);
    when(end.hasAnyOf(anyCollection())).thenReturn(query);
  }

  /**
   * Should list hosts.
   */
  @Test
  public void shouldListHosts() {
    PageResponse<Host> pageResponse = new PageResponse<>();
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
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();
    when(query.get()).thenReturn(host);
    ApplicationHost savedHost = hostService.get(APP_ID, ENV_ID, HOST_ID);
    verify(query).field("appId");
    verify(end).equal(Base.GLOBAL_APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(HOST_ID);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(ApplicationHost.class);
  }

  //  /**
  //   * Should save host.
  //   */
  //  @Test
  //  public void shouldSaveHost() {
  //    Host host = builder.build();
  //    when(wingsPersistence.saveAndGet(eq(Host.class), eq(host))).thenReturn(host);
  //    Host savedHost = hostService.save(host);
  //    assertThat(savedHost).isNotNull();
  //    assertThat(savedHost).isInstanceOf(Host.class);
  //  }

  /**
   * Should update host.
   */
  @Test
  public void shouldUpdateHost() {
    // TODO: Infra
    //    Host host = builder.withUuid(HOST_ID).build();
    //    when(wingsPersistence.saveAndGet(eq(ApplicationHost.class), eq(host))).thenReturn(host);
    //    when(tagService.getDefaultTagForUntaggedHosts(APP_ID, ENV_ID))
    //        .thenReturn(aTag().withAppId(APP_ID).withEnvId(ENV_ID).withTagType(TagType.UNTAGGED_HOST).build());
    //    ApplicationHost savedHost = hostService.update(ENV_ID, host);
    //    assertThat(savedHost).isNotNull();
    //    assertThat(savedHost).isInstanceOf(ApplicationHost.class);
  }

  /**
   * Should delete host.
   */
  @Test
  public void shouldDeleteHost() {
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();
    when(query.get()).thenReturn(host);
    when(wingsPersistence.delete(any(Host.class))).thenReturn(true);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    hostService.delete(APP_ID, ENV_ID, HOST_ID);
    verify(query).field("appId");
    verify(end).equal(Base.GLOBAL_APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(HOST_ID);
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
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();
    when(query.asList()).thenReturn(asList(host));
    when(wingsPersistence.delete(any(Host.class))).thenReturn(true);
    hostService.deleteByInfra(INFRA_ID);

    verify(query).asList();
    verify(query).field("appId");
    verify(end).equal(Base.GLOBAL_APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(wingsPersistence).delete(host);
    verify(serviceTemplateService).deleteHostFromTemplates(host);
  }

  /**
   * Should get hosts by tags.
   */
  @Test
  public void shouldGetHostsByTags() {
    List<Tag> tags = asList(aTag().withUuid(TAG_ID).build());
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();
    when(query.asList()).thenReturn(asList(host));

    List<ApplicationHost> hosts = hostService.getHostsByTags(APP_ID, ENV_ID, tags);

    verify(infrastructureService).getInfraByEnvId(ENV_ID);
    verify(query).asList();
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field("configTag");
    verify(end).hasAnyOf(tags);
    assertThat(hosts.get(0)).isInstanceOf(Host.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  /**
   * Should get hosts by host ids.
   */
  @Test
  public void shouldGetHostsByHostIds() {
    ApplicationHost host = anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();
    when(query.asList()).thenReturn(asList(host));

    List<ApplicationHost> hosts = hostService.getHostsByHostIds(APP_ID, ENV_ID, asList(HOST_ID));

    verify(query).asList();
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field(ID_KEY);
    verify(end).hasAnyOf(asList(HOST_ID));
    assertThat(hosts.get(0)).isInstanceOf(Host.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  /**
   * Should bulk save.
   */
  @Test
  @Ignore
  public void shouldBulkSave() {
    Tag tag = aTag().withUuid(TAG_ID).build();
    ServiceTemplate serviceTemplate = aServiceTemplate().withUuid(TEMPLATE_ID).build();
    SettingAttribute hostConnAttr =
        aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();

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
                           .withAppId(APP_ID)
                           .withInfraId(INFRA_ID)
                           .withHostName(HOST_NAME)
                           .withHostConnAttr(hostConnAttr)
                           .withConfigTag(tag)
                           .build();
    Host hostPostSave = aHost()
                            .withUuid(HOST_ID)
                            .withAppId(APP_ID)
                            .withInfraId(INFRA_ID)
                            .withHostName(HOST_NAME)
                            .withHostConnAttr(hostConnAttr)
                            .withConfigTag(tag)
                            .build();

    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().withName("PROD").build());
    when(serviceTemplateService.get(APP_ID, ENV_ID, TEMPLATE_ID, true)).thenReturn(serviceTemplate);
    when(tagService.get(APP_ID, ENV_ID, TAG_ID)).thenReturn(tag);
    when(wingsPersistence.saveAndGet(Host.class, hostPreSave)).thenReturn(hostPostSave);
    when(infrastructureService.get(INFRA_ID))
        .thenReturn(aStaticInfrastructure().withAppId(Base.GLOBAL_APP_ID).withUuid(INFRA_ID).build());

    hostService.bulkSave(INFRA_ID, ENV_ID, requestHost);

    // TODO:: Infra
    //    verify(wingsPersistence).saveAndGet(Host.class, hostPreSave);
    //    verify(serviceTemplateService).get(APP_ID, ENV_ID, TEMPLATE_ID, true);
    //    verify(tagService).get(APP_ID, ENV_ID, TAG_ID);
    //    verify(serviceTemplateService).addHosts(serviceTemplate, asList(hostPostSave));
    //    verify(notificationService).sendNotificationAsync(any(Notification.class));
  }

  /**
   * Should remove tag from host.
   */
  @Test
  public void shouldRemoveTagFromHost() {
    Tag tag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Tag defaultTag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Host host = builder.withUuid(HOST_ID).withConfigTag(aTag().withRootTagId("TAG_ID_1").build()).build();
    ApplicationHost applicationHost =
        anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build();

    when(tagService.getDefaultTagForUntaggedHosts(APP_ID, ENV_ID)).thenReturn(defaultTag);
    when(updateOperations.set("configTag", tag)).thenReturn(updateOperations);
    hostService.removeTagFromHost(applicationHost, tag);
    verify(wingsPersistence).createUpdateOperations(Host.class);
    verify(updateOperations).set("configTag", defaultTag);
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
                               .withHost(builder.withUuid(HOST_ID).build())
                               .build();

    Tag tag = aTag().withUuid(TAG_ID).build();
    when(updateOperations.set("configTag", tag)).thenReturn(updateOperations);
    hostService.setTag(host, tag);
    verify(wingsPersistence).update(host, updateOperations);
    verify(wingsPersistence).createUpdateOperations(Host.class);
    verify(updateOperations).set("configTag", tag);
  }
}
