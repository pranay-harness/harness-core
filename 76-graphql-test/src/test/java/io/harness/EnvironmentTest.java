package io.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Environment;
import software.wings.graphql.schema.type.QLPipeline;

@Slf4j
public class EnvironmentTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject EnvironmentGenerator environmentGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryEnvironment() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);

    String query = "{ environment(environmentId: \"" + environment.getUuid() + "\") { id name description type } }";

    QLPipeline qlPipeline = qlExecute(QLPipeline.class, query);
    assertThat(qlPipeline.getId()).isEqualTo(environment.getUuid());
    assertThat(qlPipeline.getName()).isEqualTo(environment.getName());
    assertThat(qlPipeline.getDescription()).isEqualTo(environment.getDescription());
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
}
