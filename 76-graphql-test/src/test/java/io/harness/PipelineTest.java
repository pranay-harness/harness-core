package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineBuilder;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipelineConnection;

@Slf4j
public class PipelineTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject PipelineGenerator pipelineGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryPipeline() throws InstantiationException, IllegalAccessException {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Pipeline pipeline = pipelineGenerator.ensurePredefined(seed, owners, BARRIER);

    String query = "{ pipeline(pipelineId: \"" + pipeline.getUuid() + "\") { id name description } }";

    QLPipeline qlPipeline = execute(QLPipeline.class, query);
    assertThat(qlPipeline.getId()).isEqualTo(pipeline.getUuid());
    assertThat(qlPipeline.getName()).isEqualTo(pipeline.getName());
    assertThat(qlPipeline.getDescription()).isEqualTo(pipeline.getDescription());
  }

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryPipelines() throws InstantiationException, IllegalAccessException {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().withName("Application Pipelines").build());

    final PipelineBuilder builder = Pipeline.builder().name("pipeline").appId(application.getUuid());

    final Pipeline pipeline1 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());
    final Pipeline pipeline2 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());
    final Pipeline pipeline3 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());

    {
      String query =
          "{ pipelines(appId: \"" + application.getUuid() + "\", limit: 2) { nodes { id name description } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(pipeline3.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(pipeline2.getUuid());
    }

    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 2, offset: 1) { nodes { id name description } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(pipeline2.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(pipeline1.getUuid());
    }
  }
}
