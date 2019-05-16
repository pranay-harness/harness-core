package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.graphql.schema.type.QLService.QLServiceKeys;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceBuilder;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLServiceConnection;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;

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
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    assertThat(service).isNotNull();

    String query = $GQL(/*
{
  service(serviceId: "%s") {
    id
    name
    description
    artifactType
    deploymentType
    createdAt
    createdBy {
      id
      name
      email
    }
  }
}*/ service.getUuid());

    QLTestObject qlService = qlExecute(query);
    assertThat(qlService.get(QLServiceKeys.id)).isEqualTo(service.getUuid());
    assertThat(qlService.get(QLServiceKeys.name)).isEqualTo(service.getName());
    assertThat(qlService.get(QLServiceKeys.description)).isEqualTo(service.getDescription());
    assertThat(qlService.get(QLServiceKeys.artifactType)).isEqualTo(service.getArtifactType().name());
    assertThat(qlService.get(QLServiceKeys.deploymentType)).isEqualTo(service.getDeploymentType().name());
    assertThat(qlService.get(QLServiceKeys.createdAt))
        .isEqualTo(GraphQLDateTimeScalar.convertToString(service.getCreatedAt()));
    assertThat(qlService.sub(QLServiceKeys.createdBy).get(QLUserKeys.id)).isEqualTo(service.getCreatedBy().getUuid());
    assertThat(qlService.sub(QLServiceKeys.createdBy).get(QLUserKeys.name)).isEqualTo(service.getCreatedBy().getName());
    assertThat(qlService.sub(QLServiceKeys.createdBy).get(QLUserKeys.email))
        .isEqualTo(service.getCreatedBy().getEmail());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryServices() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().name("Service App").build());

    final ServiceBuilder serviceBuilder = Service.builder().appId(application.getUuid());

    final Service service1 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service1").uuid(UUIDGenerator.generateUuid()).build());
    final Service service2 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service2").uuid(UUIDGenerator.generateUuid()).build());
    final Service service3 = serviceGenerator.ensureService(
        seed, owners, serviceBuilder.name("Service3").uuid(UUIDGenerator.generateUuid()).build());

    {
      String query = $GQL(/*
{
  services(applicationId: "%s" limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLServiceConnection serviceConnection = qlExecute(QLServiceConnection.class, query);
      assertThat(serviceConnection.getNodes().size()).isEqualTo(2);

      assertThat(serviceConnection.getNodes().get(0).getId()).isEqualTo(service3.getUuid());
      assertThat(serviceConnection.getNodes().get(1).getId()).isEqualTo(service2.getUuid());
    }

    {
      String query = $GQL(/*
{
  services(applicationId: "%s" limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLServiceConnection serviceConnection = qlExecute(QLServiceConnection.class, query);
      assertThat(serviceConnection.getNodes().size()).isEqualTo(2);

      assertThat(serviceConnection.getNodes().get(0).getId()).isEqualTo(service2.getUuid());
      assertThat(serviceConnection.getNodes().get(1).getId()).isEqualTo(service1.getUuid());
    }

    {
      String query = $GQL(/*
{
  application(applicationId: "%s") {
    services(limit: 5) {
      nodes {
        id
      }
    }
  }
}*/ application.getUuid());
      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.getMap().size()).isEqualTo(1);
    }
  }
}
