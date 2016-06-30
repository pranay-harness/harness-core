package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Host;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.SearchFilter;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
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
  @Mock private InfraService infraService;
  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @InjectMocks private HostService hostService;

  @Mock private Query<Host> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<Host> updateOperations;

  private SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private HostBuilder builder = aHost()
                                    .withAppId(APP_ID)
                                    .withInfraId(INFRA_ID)
                                    .withHostName(HOST_NAME)
                                    .withHostConnAttr(HOST_CONN_ATTR_PWD)
                                    .withHostConnectionCredential(CREDENTIAL);

  @Before
  public void setUp() throws Exception {
    when(infraService.getInfraIdByEnvId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);

    when(wingsPersistence.createUpdateOperations(Host.class)).thenReturn(updateOperations);
    when(wingsPersistence.createQuery(Host.class)).thenReturn(query);
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
    Host host = builder.withUuid(HOST_ID).build();
    when(query.get()).thenReturn(host);
    Host savedHost = hostService.get(APP_ID, INFRA_ID, HOST_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(HOST_ID);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  /**
   * Should save host.
   */
  @Test
  public void shouldSaveHost() {
    Host host = builder.build();
    when(wingsPersistence.saveAndGet(eq(Host.class), eq(host))).thenReturn(host);
    Host savedHost = hostService.save(host);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  /**
   * Should update host.
   */
  @Test
  public void shouldUpdateHost() {
    Host host = builder.withUuid(HOST_ID).build();
    when(wingsPersistence.saveAndGet(eq(Host.class), eq(host))).thenReturn(host);
    Host savedHost = hostService.update(host);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  /**
   * Should delete host.
   */
  @Test
  public void shouldDeleteHost() {
    Host host = builder.withUuid(HOST_ID).build();
    when(query.get()).thenReturn(host);
    hostService.delete(APP_ID, INFRA_ID, HOST_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(HOST_ID);
    verify(wingsPersistence).delete(host);
    verify(serviceTemplateService).deleteHostFromTemplates(host);
  }

  @Test
  public void shouldDeleteByInfra() {
    Host host = builder.withAppId(APP_ID).withInfraId(INFRA_ID).withUuid(HOST_ID).build();
    when(query.asList()).thenReturn(asList(host));
    hostService.deleteByInfra(APP_ID, INFRA_ID);

    verify(query).asList();
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(wingsPersistence).delete(host);
    verify(serviceTemplateService).deleteHostFromTemplates(host);
  }

  @Test
  public void shouldGetHostsByTags() {
    List<Tag> tags = asList(Tag.Builder.aTag().withUuid(TAG_ID).build());
    when(query.asList()).thenReturn(asList(aHost().withUuid(HOST_ID).build()));

    List<Host> hosts = hostService.getHostsByTags(APP_ID, ENV_ID, tags);

    verify(infraService).getInfraIdByEnvId(APP_ID, ENV_ID);
    verify(query).asList();
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("infraId");
    verify(end).equal(INFRA_ID);
    verify(query).field("tags");
    verify(end).hasAnyOf(tags);
    assertThat(hosts.get(0)).isInstanceOf(Host.class);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }

  @Test
  public void shouldGetHostsByHostIds() {
    when(query.asList()).thenReturn(asList(aHost().withUuid(HOST_ID).build()));

    List<Host> hosts = hostService.getHostsByHostIds(APP_ID, INFRA_ID, asList(HOST_ID));

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

  @Test
  public void shouldBulkSave() {
    HostBuilder hostBuilder = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withHostConnAttr(
        aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build());
    hostService.bulkSave(hostBuilder.build(), asList(HOST_NAME));
    verify(wingsPersistence).saveAndGet(Host.class, hostBuilder.withHostName(HOST_NAME).build());
  }

  @Test
  public void shouldRemoveTagFromHost() {
    Host host = builder.withUuid(HOST_ID).build();
    Tag tag = Tag.Builder.aTag().withUuid(TAG_ID).build();
    when(updateOperations.removeAll("tags", tag)).thenReturn(updateOperations);
    hostService.removeTagFromHost(host, tag);
    verify(wingsPersistence).update(host, updateOperations);
    verify(wingsPersistence).createUpdateOperations(Host.class);
    verify(updateOperations).removeAll("tags", tag);
  }

  @Test
  public void shouldSetTags() {
    Host host = builder.withUuid(HOST_ID).build();
    Tag tag = Tag.Builder.aTag().withUuid(TAG_ID).build();
    when(updateOperations.set("tags", asList(tag))).thenReturn(updateOperations);
    hostService.setTags(host, asList(tag));
    verify(wingsPersistence).update(host, updateOperations);
    verify(wingsPersistence).createUpdateOperations(Host.class);
    verify(updateOperations).set("tags", asList(tag));
  }
}
