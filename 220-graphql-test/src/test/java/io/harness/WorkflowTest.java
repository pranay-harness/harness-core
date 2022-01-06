/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.WorkflowGenerator.Workflows.BASIC_SIMPLE;
import static io.harness.rule.OwnerRule.GEORGE;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Application;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.graphql.schema.type.QLTag.QLTagKeys;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowKeys;
import software.wings.graphql.schema.type.QLWorkflowConnection;
import software.wings.infra.InfrastructureDefinition;
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
public class WorkflowTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject WorkflowGenerator workflowGenerator;
  @Inject InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private HarnessTagService harnessTagService;

  @Test
  @Owner(developers = GEORGE, intermittent = true)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflow() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final Workflow workflow = workflowGenerator.ensurePredefined(seed, owners, BASIC_SIMPLE);

    {
      String query = $GQL(/*
{
  workflow(workflowId: "%s") {
    id
    name
    description
    createdAt
    createdBy {
      id
    }
  }
}*/ workflow.getUuid());

      QLTestObject qlWorkflow = qlExecute(query, workflow.getAccountId());
      assertThat(qlWorkflow.get(QLWorkflowKeys.id)).isEqualTo(workflow.getUuid());
      assertThat(qlWorkflow.get(QLWorkflowKeys.name)).isEqualTo(workflow.getName());
      assertThat(qlWorkflow.get(QLWorkflowKeys.description)).isEqualTo(workflow.getDescription());
      assertThat(qlWorkflow.get(QLWorkflowKeys.createdAt)).isEqualTo(workflow.getCreatedAt());
      assertThat(qlWorkflow.sub(QLWorkflowKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(workflow.getCreatedBy().getUuid());
    }

    {
      String query = $GQL(/*
{
  workflow(workflowId: "%s") {
    id
    tags {
      name
      value
    }
  }
}*/ workflow.getUuid());

      attachTag(workflow);
      QLTestObject qlWorkflow = qlExecute(query, workflow.getAccountId());
      assertThat(qlWorkflow.get(QLWorkflowKeys.id)).isEqualTo(workflow.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) qlWorkflow.get("tags")).get(0));
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
  workflow(workflowId: "blah") {
    id
  }
}*/);

    final ExecutionResult result = qlResult(query, getAccountId());
    assertThat(result.getErrors().size()).isEqualTo(1);
    assertThat(result.getErrors().get(0).getMessage())
        .isEqualTo("Exception while fetching data (/workflow) : Entity with id: blah is not found");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryWorkflows() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();

    final Application application =
        applicationGenerator.ensureApplication(seed, owners, anApplication().name("Application Workflows").build());
    owners.add(application);

    String accountId = application.getAccountId();
    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureDefinitions.AWS_SSH_TEST);

    final WorkflowBuilder builder =
        aWorkflow()
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                       .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                       .build());

    final Workflow workflow1 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow1").build());
    final Workflow workflow2 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow2").build());
    final Workflow workflow3 = workflowGenerator.ensureWorkflow(seed, owners, builder.name("workflow3").build());

    {
      String query = $GQL(/*
{
  workflows(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query, accountId);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow3.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow2.getUuid());
    }
    {
      String query = $GQL(/*
{
  workflows(filters:[{application:{operator:EQUALS,values:["%s"]}}]  limit: 2 offset: 1) {
    nodes {
      id
    }
  }
}*/ application.getUuid());

      QLWorkflowConnection workflowConnection = qlExecute(QLWorkflowConnection.class, query, accountId);
      assertThat(workflowConnection.getNodes().size()).isEqualTo(2);

      assertThat(workflowConnection.getNodes().get(0).getId()).isEqualTo(workflow2.getUuid());
      assertThat(workflowConnection.getNodes().get(1).getId()).isEqualTo(workflow1.getUuid());
    }

    {
      String query = $GQL(/*
{
application(applicationId: "%s") {
  workflows(limit: 5) {
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
  workflows(filters:[{workflow:{operator:IN,values:["%s"]}}] limit: 1) {
    nodes {
      id
      tags {
        name
        value
      }
    }
  }
}*/ workflow1.getUuid());

      attachTag(workflow1);
      QLTestObject workflowConnection = qlExecute(query, application.getAccountId());
      Map<String, Object> workflowMap = (LinkedHashMap) (((ArrayList) workflowConnection.get("nodes")).get(0));
      assertThat(workflowMap.get(QLWorkflowKeys.id)).isEqualTo(workflow1.getUuid());
      Map<String, String> tagsMap = (LinkedHashMap) (((ArrayList) workflowMap.get("tags")).get(0));
      assertThat(tagsMap.get(QLTagKeys.name)).isEqualTo("color");
      assertThat(tagsMap.get(QLTagKeys.value)).isEqualTo("red");
    }
  }

  private void attachTag(Workflow workflow) {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(workflow.getAccountId())
                                    .appId(workflow.getAppId())
                                    .entityId(workflow.getUuid())
                                    .entityType(WORKFLOW)
                                    .key("color")
                                    .value("red")
                                    .build());
  }
}
