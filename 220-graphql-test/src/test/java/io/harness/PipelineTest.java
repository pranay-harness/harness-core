/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EntityType.PIPELINE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Application;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineBuilder;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineKeys;
import software.wings.graphql.schema.type.QLPipelineConnection;
import software.wings.graphql.schema.type.QLTag.QLTagKeys;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import graphql.ExecutionResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class PipelineTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private HarnessTagService harnessTagService;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject PipelineGenerator pipelineGenerator;

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryPipeline() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Pipeline pipeline = pipelineGenerator.ensurePredefined(seed, owners, BARRIER);

    {
      String query = $GQL(/*
{
  pipeline(pipelineId: "%s") {
    id
    name
    description
    createdAt
    createdBy {
      id
    }
  }
}*/ pipeline.getUuid());

      QLTestObject qlPipeline = qlExecute(query, pipeline.getAccountId());
      assertThat(qlPipeline.get(QLPipelineKeys.id)).isEqualTo(pipeline.getUuid());
      assertThat(qlPipeline.get(QLPipelineKeys.name)).isEqualTo(pipeline.getName());
      assertThat(qlPipeline.get(QLPipelineKeys.description)).isEqualTo(pipeline.getDescription());
    }

    {
      String query = $GQL(/*
{
  pipeline(pipelineId: "%s") {
    id
    tags {
      name
      value
    }
  }
}*/ pipeline.getUuid());

      attachTag(pipeline);
      QLTestObject qlPipeline = qlExecute(query, pipeline.getAccountId());
      assertThat(qlPipeline.get(QLPipelineKeys.id)).isEqualTo(pipeline.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) qlPipeline.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryMissingPipeline() {
    String query = $GQL(/*
{
  pipeline(pipelineId: "blah") {
    id
  }
}*/);

    final ExecutionResult result = qlResult(query, getAccountId());
    assertThat(result.getErrors().size()).isEqualTo(1);

    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/pipeline) : Entity with id: blah is not found");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryPipelines() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Pipelines").build());
    String accountId = application.getAccountId();
    final PipelineBuilder builder = Pipeline.builder().appId(application.getUuid());

    final Pipeline pipeline1 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline1").build());
    final Pipeline pipeline2 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline2").build());
    final Pipeline pipeline3 =
        pipelineGenerator.ensurePipeline(seed, owners, builder.uuid(generateUuid()).name("pipeline3").build());

    {
      String query = $GQL(/*
{
  pipelines(filters:[{application:{operator:EQUALS,values:["%s"]}}] limit: 2) {
    nodes {
      id
      name
      description
    }
  }
}*/ application.getUuid());

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(pipeline3.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(pipeline2.getUuid());
    }

    {
      String query = $GQL(/*
{
  pipelines(filters:[{application:{operator:EQUALS,values:["%s"]}}] limit: 2 offset: 1) {
    nodes {
      id
      name
      description
    }
  }
}*/ application.getUuid());

      QLPipelineConnection pipelineConnection = qlExecute(QLPipelineConnection.class, query, accountId);
      assertThat(pipelineConnection.getNodes().size()).isEqualTo(2);

      assertThat(pipelineConnection.getNodes().get(0).getId()).isEqualTo(pipeline2.getUuid());
      assertThat(pipelineConnection.getNodes().get(1).getId()).isEqualTo(pipeline1.getUuid());
    }

    {
      String query = $GQL(/*
{
  application(applicationId: "%s") {
    pipelines(limit: 2 offset: 1) {
      nodes {
        id
      }
    }
  }
}*/ application.getUuid());

      final QLTestObject qlTestObject = qlExecute(query, accountId);
      assertThat(qlTestObject.getMap().size()).isEqualTo(1);
    }

    {
      String query = $GQL(/*
{
  pipelines(filters:[{pipeline:{operator:IN,values:["%s"]}}] limit: 1) {
    nodes {
      id
      tags {
        name
        value
      }
    }
  }
}*/ pipeline1.getUuid());

      attachTag(pipeline1);
      QLTestObject pipelineConnection = qlExecute(query, application.getAccountId());
      Map<String, Object> pipelineMap = (LinkedHashMap) (((ArrayList) pipelineConnection.get("nodes")).get(0));
      assertThat(pipelineMap.get(QLPipelineKeys.id)).isEqualTo(pipeline1.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) pipelineMap.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  private void attachTag(Pipeline pipeline) {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(pipeline.getAccountId())
                                    .appId(pipeline.getAppId())
                                    .entityId(pipeline.getUuid())
                                    .entityType(PIPELINE)
                                    .key("color")
                                    .value("red")
                                    .build());
  }
}
