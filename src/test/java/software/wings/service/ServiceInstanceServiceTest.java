package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Service.ServiceBuilder.aService;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Artifact.Builder;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.Release.ReleaseBuilder;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstance.ServiceInstanceBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/26/16.
 */

public class ServiceInstanceServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private ServiceInstanceService serviceInstanceService;

  private ServiceInstanceBuilder builder = aServiceInstance()
                                               .withHost(HostBuilder.aHost().withUuid("HOST_ID").build())
                                               .withService(aService().withUuid("SERVICE_ID").build())
                                               .withServiceTemplate(aServiceTemplate().withUuid("TEMPLATE_ID").build())
                                               .withRelease(ReleaseBuilder.aRelease().withUuid("RELEASE_ID").build())
                                               .withArtifact(Builder.anArtifact().withUuid("ARTIFACT_ID").build())
                                               .withAppId("APP_ID")
                                               .withEnvId("ENV_ID");

  @Test
  public void shouldSaveServiceInstance() {
    ServiceInstance serviceInstance = builder.build();
    when(wingsPersistence.saveAndGet(eq(ServiceInstance.class), eq(serviceInstance))).thenReturn(serviceInstance);
    ServiceInstance savedServiceInstance = serviceInstanceService.save(serviceInstance);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  @Test
  public void shouldUpdateServiceInstance() {
    ServiceInstance serviceInstance = builder.withUuid("INSTANCE_ID").build();
    when(wingsPersistence.saveAndGet(eq(ServiceInstance.class), eq(serviceInstance))).thenReturn(serviceInstance);
    ServiceInstance savedServiceInstance = serviceInstanceService.update(serviceInstance);
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  @Test
  public void shouldGetServiceInstance() {
    ServiceInstance serviceInstance = builder.withUuid("INSTANCE_ID").build();
    when(wingsPersistence.get(ServiceInstance.class, "INSTANCE_ID")).thenReturn(serviceInstance);

    ServiceInstance savedServiceInstance = serviceInstanceService.get("APP_ID", "ENV_ID", "INSTANCE_ID");
    assertThat(savedServiceInstance).isNotNull();
    assertThat(savedServiceInstance).isInstanceOf(ServiceInstance.class);
  }

  @Test
  public void shouldDeleteServiceInstance() {
    serviceInstanceService.delete("APP_ID", "ENV_ID", "INSTANCE_ID");
    verify(wingsPersistence).delete(ServiceInstance.class, "INSTANCE_ID");
  }
}
