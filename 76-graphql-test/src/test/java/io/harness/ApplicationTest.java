package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import graphql.ExecutionResult;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipelineConnection;

@Slf4j
public class ApplicationTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject AccountGenerator accountGenerator;
  @Inject ApplicationGenerator applicationGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryApplication() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);

    String query = "{ application(applicationId: \"" + application.getUuid() + "\") { id name description } }";

    QLPipeline qlPipeline = qlExecute(QLPipeline.class, query);
    assertThat(qlPipeline.getId()).isEqualTo(application.getUuid());
    assertThat(qlPipeline.getName()).isEqualTo(application.getName());
    assertThat(qlPipeline.getDescription()).isEqualTo(application.getDescription());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingApplication() {
    String query = "{ application(applicationId: \"blah\") { id name description } }";

    final ExecutionResult result = getGraphQL().execute(query);
    assertThat(result.getErrors().size()).isEqualTo(1);

    // TODO: this message is wrong
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/application) : INVALID_REQUEST");
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryPipelines() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Account account = accountGenerator.getOrCreateAccount(generateUuid(), "testing", "graphql");
    accountGenerator.ensureAccount(account);
    owners.add(account);

    final Application application1 = applicationGenerator.ensureApplication(
        seed, owners, anApplication().withName("Application - " + generateUuid()).build());
    final Application application2 = applicationGenerator.ensureApplication(
        seed, owners, anApplication().withName("Application - " + generateUuid()).build());
    final Application application3 = applicationGenerator.ensureApplication(
        seed, owners, anApplication().withName("Application - " + generateUuid()).build());

    {
      String query =
          "{ applications(accountId: \"" + account.getUuid() + "\", limit: 2) { nodes { id name description } } }";

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(application3.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(application2.getUuid());
    }

    {
      String query = "{ applications(accountId: \"" + account.getUuid()
          + "\", limit: 2, offset: 1) { nodes { id name description } } }";

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(application2.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(application1.getUuid());
    }
  }
}
