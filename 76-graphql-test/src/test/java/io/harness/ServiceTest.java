package io.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceBuilder;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLServiceConnection;

@Slf4j
public class ServiceTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ApplicationGenerator applicationGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryService() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    assertThat(service).isNotNull();

    String query = "{ service(serviceId: \"" + service.getUuid() + "\") { id name description artifactType } }";

    QLService qlService = qlExecute(QLService.class, query);
    assertThat(qlService.getId()).isEqualTo(service.getUuid());
    assertThat(qlService.getName()).isEqualTo(service.getName());
    assertThat(qlService.getDescription()).isEqualTo(service.getDescription());
    assertThat(qlService.getArtifactType()).isEqualTo(service.getArtifactType());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryServices() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().withName("Service App").build());

    final ServiceBuilder serviceBuilder = Service.builder().appId(application.getUuid());

    final Service service1 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service1").uuid(UUIDGenerator.generateUuid()).build());
    final Service service2 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service2").uuid(UUIDGenerator.generateUuid()).build());
    final Service service3 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service3").uuid(UUIDGenerator.generateUuid()).build());

    String query = "{ services(appId: \"" + application.getUuid()
        + "\", limit: 2) { nodes { id name description artifactType deploymentType} } }";

    QLServiceConnection serviceConnection = qlExecute(QLServiceConnection.class, query);
    assertThat(serviceConnection.getNodes().size()).isEqualTo(2);

    assertThat(serviceConnection.getNodes().get(0).getId()).isEqualTo(service3.getUuid());
    assertThat(serviceConnection.getNodes().get(1).getId()).isEqualTo(service2.getUuid());

    query = "{ services(appId: \"" + application.getUuid()
        + "\", limit: 2, offset: 1) { nodes { id name description artifactType, deploymentType} } }";

    serviceConnection = qlExecute(QLServiceConnection.class, query);
    assertThat(serviceConnection.getNodes().size()).isEqualTo(2);

    assertThat(serviceConnection.getNodes().get(0).getId()).isEqualTo(service2.getUuid());
    assertThat(serviceConnection.getNodes().get(1).getId()).isEqualTo(service1.getUuid());
  }
}
