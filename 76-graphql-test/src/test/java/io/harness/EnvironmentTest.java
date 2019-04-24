package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironmentConnection;

@Slf4j
public class EnvironmentTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject EnvironmentGenerator environmentGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironment() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    String query = "{ environment(environmentId: \"" + environment.getUuid() + "\") { id name description type } }";

    QLEnvironment qlEnvironment = qlExecute(QLEnvironment.class, query);
    assertThat(qlEnvironment.getId()).isEqualTo(environment.getUuid());
    assertThat(qlEnvironment.getName()).isEqualTo(environment.getName());
    assertThat(qlEnvironment.getDescription()).isEqualTo(environment.getDescription());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingEnvironment() {
    String query = "{ environment(environmentId: \"blah\") { id name description type } }";

    final ExecutionResult result = getGraphQL().execute(query);
    assertThat(result.getErrors().size()).isEqualTo(1);

    // TODO: this message is wrong
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/environment) : INVALID_REQUEST");
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironments() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, anApplication().withName("Application Environments").build());

    final Builder builder = anEnvironment().withAppId(application.getUuid());

    final Environment environment1 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.withUuid(generateUuid()).withName("Environment - " + generateUuid()).build());
    final Environment environment2 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.withUuid(generateUuid()).withName("Environment - " + generateUuid()).build());
    final Environment environment3 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.withUuid(generateUuid()).withName("Environment - " + generateUuid()).build());

    {
      String query = "{ environments(applicationId: \"" + application.getUuid()
          + "\", limit: 2) { nodes { id name description } } }";

      QLEnvironmentConnection environmentConnection = qlExecute(QLEnvironmentConnection.class, query);
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment3.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment2.getUuid());
    }

    {
      String query = "{ environments(applicationId: \"" + application.getUuid()
          + "\", limit: 2, offset: 1) { nodes { id name description } } }";

      QLEnvironmentConnection environmentConnection = qlExecute(QLEnvironmentConnection.class, query);
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment2.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment1.getUuid());
    }
  }
}
