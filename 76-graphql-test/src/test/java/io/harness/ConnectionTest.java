package io.harness;

import static graphql.Assert.assertTrue;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
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
import software.wings.graphql.schema.type.QLPipelineConnection;

@Slf4j
public class ConnectionTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject PipelineGenerator pipelineGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testConnectionPaging() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().withName("Application Pipelines").build());

    final PipelineBuilder builder = Pipeline.builder().name("pipeline").appId(application.getUuid());

    final Pipeline pipeline1 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());
    final Pipeline pipeline2 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());
    final Pipeline pipeline3 = pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).build());

    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 2) { nodes { id } pageInfo { limit offset hasMore total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertTrue(pipelineConnection.getPageInfo().getHasMore());
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 2, offset: 1) { nodes { id } pageInfo { limit offset hasMore total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(1);
      assertFalse(pipelineConnection.getPageInfo().getHasMore());
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 0) { nodes { id } pageInfo { limit offset hasMore total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(3);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertFalse(pipelineConnection.getPageInfo().getHasMore());
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 4) { nodes { id } pageInfo { limit offset hasMore total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(4);
      assertFalse(pipelineConnection.getPageInfo().getHasMore());
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 0, offset: 0) { nodes { id } pageInfo { limit offset hasMore total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertTrue(pipelineConnection.getPageInfo().getHasMore());
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 0) { nodes { id } pageInfo { limit offset total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(3);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 2) { nodes { id } pageInfo { limit offset total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(1);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 3) { nodes { id } pageInfo { limit offset total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(3);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = "{ pipelines(appId: \"" + application.getUuid()
          + "\", limit: 5, offset: 5) { nodes { id } pageInfo { limit offset total } } }";

      QLPipelineConnection pipelineConnection = execute(QLPipelineConnection.class, query);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
  }
}
