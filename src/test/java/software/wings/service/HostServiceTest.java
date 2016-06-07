package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Host;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.SearchFilter;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
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

  @Inject @InjectMocks private HostService hostService;

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

  @Test
  public void shouldSaveHost() {
    Host host = builder.build();
    when(wingsPersistence.saveAndGet(eq(Host.class), eq(host))).thenReturn(host);
    Host savedHost = hostService.save(host);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  @Test
  public void shouldUpdateHost() {
    Host host = builder.withUuid(HOST_ID).build();
    when(wingsPersistence.saveAndGet(eq(Host.class), eq(host))).thenReturn(host);
    Host savedHost = hostService.update(host);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  @Test
  public void shouldGetHost() {
    Host host = builder.withUuid(HOST_ID).build();
    when(wingsPersistence.get(Host.class, HOST_ID)).thenReturn(host);

    Host savedHost = hostService.get(APP_ID, INFRA_ID, HOST_ID);
    assertThat(savedHost).isNotNull();
    assertThat(savedHost).isInstanceOf(Host.class);
  }

  @Test
  public void shouldDeleteHost() {
    hostService.delete(APP_ID, INFRA_ID, HOST_ID);
    verify(wingsPersistence).delete(Host.class, HOST_ID);
  }

  @Test
  @Ignore
  public void shouldGetHostById() {
    when(wingsPersistence.createQuery(Host.class)
             .field("appId")
             .equal(APP_ID)
             .field(ID_KEY)
             .hasAnyOf(asList(HOST_ID))
             .asList())
        .thenReturn(asList(builder.withUuid(HOST_ID).build()));
    List<Host> hosts = hostService.getHostsById(APP_ID, asList(HOST_ID));
    assertThat(hosts).isNotNull();
    assertThat(hosts.size()).isEqualTo(1);
    assertThat(hosts.get(0).getUuid()).isEqualTo(HOST_ID);
  }
}
