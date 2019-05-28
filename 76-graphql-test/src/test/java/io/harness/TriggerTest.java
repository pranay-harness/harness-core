package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.PipelineGenerator.Pipelines.BARRIER;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerKeys;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;

import com.google.inject.Inject;

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
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection;
import software.wings.service.impl.trigger.TriggerGenerator;
import software.wings.service.intfc.TriggerService;

@Slf4j
public class TriggerTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private TriggerGenerator triggerGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject TriggerService triggerService;
  @Inject ArtifactStreamManager artifactStreamManager;
  @Inject ApplicationGenerator applicationGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryTrigger() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().name("Artifact App").build());
    assertThat(application).isNotNull();

    owners.add(application);

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

    QLTestObject qlTrigger = qlExecute(query);
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

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryTriggers() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Application application = applicationGenerator.ensureApplication(
        seed, owners, Application.Builder.anApplication().name("Artifact App").build());
    assertThat(application).isNotNull();

    owners.add(application);

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
  triggers(applicationId: "%s" limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLTriggerConnection triggerConnection = qlExecute(QLTriggerConnection.class, query);
      assertThat(triggerConnection.getNodes().size()).isEqualTo(2);

      assertThat(triggerConnection.getNodes().get(0).getId()).isEqualTo(trigger3.getUuid());
      assertThat(triggerConnection.getNodes().get(1).getId()).isEqualTo(trigger2.getUuid());
    }

    {
      String query = $GQL(/*
{
  triggers(applicationId: "%s" limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLTriggerConnection triggerConnection = qlExecute(QLTriggerConnection.class, query);
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

      final QLTestObject qlTestObject = qlExecute(query);
      assertThat(qlTestObject.getMap().size()).isEqualTo(1);
    }
  }
}
