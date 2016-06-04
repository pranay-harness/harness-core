package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsUnitTestConstants.APP_ID;
import static software.wings.utils.WingsUnitTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsUnitTestConstants.ENV_ID;
import static software.wings.utils.WingsUnitTestConstants.HOST_ID;
import static software.wings.utils.WingsUnitTestConstants.RELEASE_ID;
import static software.wings.utils.WingsUnitTestConstants.SERVICE_ID;
import static software.wings.utils.WingsUnitTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsUnitTestConstants.TEMPLATE_ID;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Artifact.Builder;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.Release.ReleaseBuilder;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstance.ServiceInstanceBuilder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/26/16.
 */

public class ServiceInstanceServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private ServiceInstanceService serviceInstanceService;

  private ServiceInstanceBuilder builder = aServiceInstance()
                                               .withHost(HostBuilder.aHost().withUuid(HOST_ID).build())
                                               .withService(aService().withUuid(SERVICE_ID).build())
                                               .withServiceTemplate(aServiceTemplate().withUuid(TEMPLATE_ID).build())
                                               .withRelease(ReleaseBuilder.aRelease().withUuid(RELEASE_ID).build())
                                               .withArtifact(Builder.anArtifact().withUuid(ARTIFACT_ID).build())
                                               .withAppId(APP_ID)
                                               .withEnvId(ENV_ID);

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
  public void shouldUpdateServiceInstance() {
    ServiceInstance serviceInstance = builder.withUuid(SERVICE_INSTANCE_ID).build();
    when(wingsPersistence.saveAndGet(eq(ServiceInstance.class), eq(serviceInstance))).thenReturn(serviceInstance);
    ServiceInstance savedServiceInstance = serviceInstanceService.update(serviceInstance);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  /**
   * Should get service instance.
   */
  @Test
  public void shouldGetServiceInstance() {
    ServiceInstance serviceInstance = builder.withUuid(SERVICE_INSTANCE_ID).build();
    when(wingsPersistence.get(ServiceInstance.class, SERVICE_INSTANCE_ID)).thenReturn(serviceInstance);

    ServiceInstance savedServiceInstance = serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  /**
   * Should delete service instance.
   */
  @Test
  public void shouldDeleteServiceInstance() {
    serviceInstanceService.delete(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(wingsPersistence).delete(ServiceInstance.class, SERVICE_INSTANCE_ID);
  }
}
