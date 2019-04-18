package io.harness;

import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Pipeline;
import software.wings.graphql.schema.type.QLPipeline;

@Slf4j
public class PipelineTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
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
}
