package io.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Service;
import software.wings.graphql.schema.type.QLService;

@Slf4j
public class ServiceTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject ServiceGenerator serviceGenerator;

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
}
