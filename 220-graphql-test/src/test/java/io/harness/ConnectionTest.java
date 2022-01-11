/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_LIMIT_ARG_MSG;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_OFFSET_ARG_MSG;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineBuilder;
import software.wings.graphql.schema.type.QLPipelineConnection;

import com.google.inject.Inject;
import graphql.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ConnectionTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject PipelineGenerator pipelineGenerator;

  private static final String pattern = $.GQL(/*
{
  pipelines(filters:[{application:{operator:EQUALS,values:["%s"]}}] limit: %d offset: %d) {
    nodes {
      id
    }
    pageInfo {
      limit
      offset
      hasMore
      total
    }
  }
}*/ CloudProviderTest.class);

  private static final String patternNoHasMore = $.GQL(/*
{
  pipelines(filters:[{application:{operator:EQUALS,values:["%s"]}}] limit: %d offset: %d) {
    nodes {
      id
    }
    pageInfo {
      limit
      offset
      total
    }
  }
}*/ CloudProviderTest.class);

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testConnectionPaging() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Pipelines").build());
    final String accountId = application.getAccountId();
    final PipelineBuilder builder = Pipeline.builder().appId(application.getUuid());

    final Pipeline pipeline1 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline1").build());
    final Pipeline pipeline2 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline2").build());
    final Pipeline pipeline3 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline3").build());

    {
      String query = format(pattern, application.getUuid(), 2, 0);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getHasMore()).isTrue();
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = format(pattern, application.getUuid(), 2, 1);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(1);
      assertThat(pipelineConnection.getPageInfo().getHasMore()).isFalse();
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = format(pattern, application.getUuid(), 5, 0);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(3);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getHasMore()).isFalse();
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = format(pattern, application.getUuid(), 5, 4);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(4);
      assertThat(pipelineConnection.getPageInfo().getHasMore()).isFalse();
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = format(pattern, application.getUuid(), 0, 0);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getHasMore()).isTrue();
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = format(pattern, application.getUuid(), 2, 0);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
    {
      String query = format(pattern, application.getUuid(), 5, 0);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(3);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(0);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = format(patternNoHasMore, application.getUuid(), 5, 2);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(1);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(2);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = format(patternNoHasMore, application.getUuid(), 5, 3);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(3);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }

    {
      String query = format(patternNoHasMore, application.getUuid(), 5, 5);

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(0);

      assertThat(pipelineConnection.getPageInfo().getLimit()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getOffset()).isEqualTo(5);
      assertThat(pipelineConnection.getPageInfo().getTotal()).isEqualTo(3);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testPagingArguments() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Pipelines").build());

    {
      String query = format(patternNoHasMore, application.getUuid(), -5, 5);

      final ExecutionResult result = qlResult(query, application.getAccountId());
      assertThat(result.getErrors().size()).isEqualTo(1);

      assertThat(result.getErrors().get(0).getMessage())
          .isEqualTo("Exception while fetching data (/pipelines) : Invalid request: " + NEGATIVE_LIMIT_ARG_MSG);
    }

    {
      String query = format(patternNoHasMore, application.getUuid(), 5, -5);

      final ExecutionResult result = qlResult(query, application.getAccountId());
      assertThat(result.getErrors().size()).isEqualTo(1);

      assertThat(result.getErrors().get(0).getMessage())
          .isEqualTo("Exception while fetching data (/pipelines) : Invalid request: " + NEGATIVE_OFFSET_ARG_MSG);
    }
  }
}
