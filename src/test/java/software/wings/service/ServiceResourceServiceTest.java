package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AppContainer.AppContainerBuilder.anAppContainer;
import static software.wings.beans.ArtifactSource.ArtifactType.JAR;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;

/**
 * Created by anubhaw on 5/4/16.
 */
public class ServiceResourceServiceTest extends WingsBaseTest {
  private final String SERVICE_ID = "SERVICE_ID";
  private final String APP_ID = "APP_ID";
  private WingsPersistence wingsPersistence = mock(WingsPersistence.class);
  private ConfigService configService = mock(ConfigService.class);
  private ServiceResourceService srs = new ServiceResourceServiceImpl(wingsPersistence, configService);
  private Builder builder = aService()
                                .withUuid(SERVICE_ID)
                                .withAppId(APP_ID)
                                .withName("SERVICE_NAME")
                                .withDescription("SERVICE_DESC")
                                .withArtifactType(JAR)
                                .withAppContainer(anAppContainer().withUuid("APP_CONTAINER_ID").build());

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(builder.build());
  }

  @Test
  public void shouldListServices() {
    PageRequest<Service> request = new PageRequest<>();
    request.addFilter("appId", APP_ID, EQ);
    srs.list(request);
    ArgumentCaptor<PageRequest> argument = ArgumentCaptor.forClass(PageRequest.class);
    verify(wingsPersistence).query(eq(Service.class), argument.capture());
    SearchFilter filter = (SearchFilter) argument.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  @Test
  public void shouldSaveService() {
    Service service = srs.save(builder.build());
    assertThat(service.getUuid()).isEqualTo(SERVICE_ID);
  }

  @Test
  public void shouldFetchService() {
    when(wingsPersistence.get(Service.class, SERVICE_ID)).thenReturn(builder.build());
    when(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(new ArrayList<ConfigFile>());
    srs.get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(Service.class, SERVICE_ID);
  }

  @Test
  public void shouldUpdateService() {
    Service service = builder.withName("UPDATED_SERVICE_NAME")
                          .withDescription("UPDATED_SERVICE_DESC")
                          .withArtifactType(WAR)
                          .withAppContainer(anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build())
                          .build();
    srs.update(service);
    verify(wingsPersistence)
        .updateFields(Service.class, SERVICE_ID,
            ImmutableMap.of("name", "UPDATED_SERVICE_NAME", "description", "UPDATED_SERVICE_DESC", "artifactType", WAR,
                "appContainer", anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build()));
  }

  @Test
  public void shouldDeleteService() {
    srs.delete(APP_ID, SERVICE_ID);
    verify(wingsPersistence).delete(Service.class, SERVICE_ID);
  }
}
