/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerKeys;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.schema.type.QLTag.QLTagKeys;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection;
import software.wings.service.impl.trigger.TriggerGenerator;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.TriggerService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class TriggerTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private TriggerGenerator triggerGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject TriggerService triggerService;
  @Inject ArtifactStreamManager artifactStreamManager;
  @Inject ApplicationGenerator applicationGenerator;
  @Inject private HarnessTagService harnessTagService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryTrigger() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().name("Artifact App").build());
    assertThat(application).isNotNull();

    owners.add(application);

    String accountId = application.getAccountId();

    final Pipeline pipeline = pipelineGenerator.ensurePredefined(seed, owners, BARRIER);
    assertThat(pipeline).isNotNull();

    final ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);

    assertThat(artifactStream).isNotNull();

    final Trigger trigger = Trigger.builder()
                                .name("New Artifact Trigger")
                                .workflowId(pipeline.getUuid())
                                .appId(application.getAppId())
                                .condition(ArtifactTriggerCondition.builder()
                                               .artifactFilter(ARTIFACT_FILTER)
                                               .artifactStreamId(artifactStream.getUuid())
                                               .build())
                                .createdBy(owners.obtainUser())
                                .build();

    Trigger savedTrigger = triggerService.save(triggerGenerator.ensureTrigger(trigger));

    assertThat(savedTrigger).isNotNull();

    {
      String query = $GQL(/*
{
  trigger(triggerId: "%s") {
    id
    name
    description
    createdAt
    createdBy {
      id
      name
      email
    }
  }
}*/ savedTrigger.getUuid());

      QLTestObject qlTrigger = qlExecute(query, accountId);
      assertThat(qlTrigger.get(QLTriggerKeys.id)).isEqualTo(savedTrigger.getUuid());
      assertThat(qlTrigger.get(QLTriggerKeys.name)).isEqualTo(savedTrigger.getName());
      assertThat(qlTrigger.get(QLTriggerKeys.description)).isEqualTo(savedTrigger.getDescription());
      assertThat(qlTrigger.sub(QLTriggerKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(savedTrigger.getCreatedBy().getUuid());
      assertThat(qlTrigger.sub(QLTriggerKeys.createdBy).get(QLUserKeys.name))
          .isEqualTo(savedTrigger.getCreatedBy().getName());
      assertThat(qlTrigger.sub(QLTriggerKeys.createdBy).get(QLUserKeys.email))
          .isEqualTo(savedTrigger.getCreatedBy().getEmail());
    }

    {
      String query = $GQL(/*
{
  trigger(triggerId: "%s") {
    id
    tags {
      name
      value
    }
  }
}*/ savedTrigger.getUuid());

      attachTag(savedTrigger, accountId);
      QLTestObject qlTrigger = qlExecute(query, accountId);
      assertThat(qlTrigger.get(QLTriggerKeys.id)).isEqualTo(savedTrigger.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) qlTrigger.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryTriggers() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().name("Artifact App").build());
    assertThat(application).isNotNull();

    owners.add(application);

    String accountId = application.getAccountId();

    final Pipeline pipeline = pipelineGenerator.ensurePredefined(seed, owners, BARRIER);
    assertThat(pipeline).isNotNull();

    final ArtifactStream artifactStream =
        artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);

    assertThat(artifactStream).isNotNull();

    Trigger trigger1 = triggerService.save(
        triggerGenerator.ensureTrigger(Trigger.builder()
                                           .name("New Artifact Trigger 1")
                                           .workflowId(pipeline.getUuid())
                                           .appId(application.getAppId())
                                           .condition(ArtifactTriggerCondition.builder()
                                                          .artifactFilter(ARTIFACT_FILTER)
                                                          .artifactStreamId(artifactStream.getUuid())
                                                          .build())
                                           .createdBy(owners.obtainUser())
                                           .build()));

    assertThat(trigger1).isNotNull();

    Trigger trigger2 = triggerService.save(
        triggerGenerator.ensureTrigger(Trigger.builder()
                                           .name("New Artifact Trigger 2")
                                           .workflowId(pipeline.getUuid())
                                           .appId(application.getAppId())
                                           .condition(ArtifactTriggerCondition.builder()
                                                          .artifactFilter(ARTIFACT_FILTER)
                                                          .artifactStreamId(artifactStream.getUuid())
                                                          .build())
                                           .createdBy(owners.obtainUser())
                                           .build()));

    assertThat(trigger2).isNotNull();

    Trigger trigger3 = triggerService.save(
        triggerGenerator.ensureTrigger(Trigger.builder()
                                           .name("New Artifact Trigger 3")
                                           .workflowId(pipeline.getUuid())
                                           .appId(application.getAppId())
                                           .condition(ArtifactTriggerCondition.builder()
                                                          .artifactFilter(ARTIFACT_FILTER)
                                                          .artifactStreamId(artifactStream.getUuid())
                                                          .build())
                                           .createdBy(owners.obtainUser())
                                           .build()));

    assertThat(trigger3).isNotNull();

    {
      String query = $GQL(/*
{
  triggers(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLTriggerConnection triggerConnection = qlExecute(QLTriggerConnection.class, query, accountId);
      assertThat(triggerConnection.getNodes().size()).isEqualTo(2);

      assertThat(triggerConnection.getNodes().get(0).getId()).isEqualTo(trigger3.getUuid());
      assertThat(triggerConnection.getNodes().get(1).getId()).isEqualTo(trigger2.getUuid());
    }

    {
      String query = $GQL(/*
{
  triggers(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLTriggerConnection triggerConnection = qlExecute(QLTriggerConnection.class, query, accountId);
      assertThat(triggerConnection.getNodes().size()).isEqualTo(2);

      assertThat(triggerConnection.getNodes().get(0).getId()).isEqualTo(trigger2.getUuid());
      assertThat(triggerConnection.getNodes().get(1).getId()).isEqualTo(trigger1.getUuid());
    }

    {
      String query = $GQL(/*
{
  application(applicationId: "%s") {
    services(limit: 5) {
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
  triggers(filters:[{trigger:{operator:IN,values:["%s"]}}] limit: 1) {
    nodes {
      id
      tags {
        name
        value
      }
    }
  }
}*/ trigger1.getUuid());

      attachTag(trigger1, accountId);
      QLTestObject triggerConnection = qlExecute(query, application.getAccountId());
      Map<String, Object> triggerMap = (LinkedHashMap) (((ArrayList) triggerConnection.get("nodes")).get(0));
      assertThat(triggerMap.get(QLTriggerKeys.id)).isEqualTo(trigger1.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) triggerMap.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  private void attachTag(Trigger trigger, String accountId) {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(accountId)
                                    .appId(trigger.getAppId())
                                    .entityId(trigger.getUuid())
                                    .entityType(EntityType.TRIGGER)
                                    .key("color")
                                    .value("red")
                                    .build());
  }
}
