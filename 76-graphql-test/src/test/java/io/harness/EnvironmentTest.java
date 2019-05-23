package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.graphql.datafetcher.environment.EnvironmentDataFetcher;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentKeys;
import software.wings.graphql.schema.type.QLEnvironmentConnection;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;

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
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    String query = $GQL(/*
{
  environment(environmentId: "%s") {
    id
    name
    description
    type
    createdAt
    createdBy {
      id
    }
  }
}*/ environment.getUuid());

    QLTestObject qlEnvironment = qlExecute(query);
    assertThat(qlEnvironment.get(QLEnvironmentKeys.id)).isEqualTo(environment.getUuid());
    assertThat(qlEnvironment.get(QLEnvironmentKeys.name)).isEqualTo(environment.getName());
    assertThat(qlEnvironment.get(QLEnvironmentKeys.description)).isEqualTo(environment.getDescription());
    assertThat(qlEnvironment.get(QLEnvironmentKeys.createdAt))
        .isEqualTo(GraphQLDateTimeScalar.convertToString(environment.getCreatedAt()));
    assertThat(qlEnvironment.sub(QLEnvironmentKeys.createdBy).get(QLUserKeys.id))
        .isEqualTo(environment.getCreatedBy().getUuid());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingEnvironment() {
    String query = $GQL(/*
{
  environment(environmentId: "blah") {
    id
  }
}*/);

    final ExecutionResult result = qlResult(query);
    assertThat(result.getErrors().size()).isEqualTo(1);

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/environment) : Invalid request: "
            + EnvironmentDataFetcher.ENV_DOES_NOT_EXISTS_MSG);
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironments() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Environments").build());

    final Builder builder = anEnvironment().appId(application.getUuid());

    final Environment environment1 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());
    final Environment environment2 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());
    final Environment environment3 = environmentGenerator.ensureEnvironment(
        seed, owners, builder.uuid(generateUuid()).name("Environment - " + generateUuid()).build());

    {
      String query = $GQL(/*
{
  environments(applicationId: "%s" limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLEnvironmentConnection environmentConnection = qlExecute(QLEnvironmentConnection.class, query);
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment3.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment2.getUuid());
    }

    {
      String query = $GQL(/*
{
  environments(applicationId: "%s" limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLEnvironmentConnection environmentConnection = qlExecute(QLEnvironmentConnection.class, query);
      assertThat(environmentConnection.getNodes().size()).isEqualTo(2);

      assertThat(environmentConnection.getNodes().get(0).getId()).isEqualTo(environment2.getUuid());
      assertThat(environmentConnection.getNodes().get(1).getId()).isEqualTo(environment1.getUuid());
    }
  }
}
