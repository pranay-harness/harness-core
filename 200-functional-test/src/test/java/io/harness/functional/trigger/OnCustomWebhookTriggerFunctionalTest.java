/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.trigger;

import static io.harness.rule.OwnerRule.MILAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ff.FeatureFlagService;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.GraphQLRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class OnCustomWebhookTriggerFunctionalTest extends AbstractFunctionalTest {
  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private OwnerManager.Owners owners;

  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowService workflowService;

  private Application application;
  private Service service;
  Workflow savedWorkflow;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    if (!featureFlagService.isEnabled(FeatureName.GRAPHQL_DEV, application.getAccountId())) {
      featureFlagService.enableAccount(FeatureName.GRAPHQL_DEV, application.getAccountId());
    }

    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
    Workflow workflow = workflowUtils.createBuildWorkflow(
        "GraphQLAPI-test-" + System.currentTimeMillis(), application.getAppId(), new ArrayList<>());
    savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
  }

  @Test
  @Owner(developers = MILAN, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldCRUDTrigger() {
    // CREATE
    String clientMutationId = "1234";
    String mutation = getGraphQLQueryForTriggerCreation(clientMutationId, application.getAppId(),
        savedWorkflow.getUuid(), service.getArtifactStreamIds().get(0), service.getUuid());
    Map<Object, Object> response =
        GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);
    assertThat(response).isNotEmpty();

    Map<String, Object> createTrigger = (Map<String, Object>) response.get("createTrigger");
    assertThat(createTrigger).isNotNull();

    assertThat(createTrigger.get("clientMutationId")).isEqualTo(clientMutationId);

    Map<String, Object> trigger = (Map<String, Object>) createTrigger.get("trigger");
    assertThat(trigger).isNotNull();
    String triggerId = (String) trigger.get("id");

    // UPDATE
    String triggerName = "updatedTriggerName1";
    mutation = getGraphQLQueryForTriggerUpdate(clientMutationId, triggerId, triggerName, application.getAppId(),
        savedWorkflow.getUuid(), service.getArtifactStreamIds().get(0), service.getUuid());
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);
    assertThat(response).isNotEmpty();

    Map<String, Object> updateTrigger = (Map<String, Object>) response.get("updateTrigger");
    assertThat(updateTrigger).isNotNull();

    assertThat(updateTrigger.get("clientMutationId")).isEqualTo(clientMutationId);

    trigger = (Map<String, Object>) updateTrigger.get("trigger");
    assertThat(trigger).isNotNull();
    assertThat((String) trigger.get("name")).isEqualTo(triggerName);

    // GET
    String query = getGraphQLQueryForTriggerRetrieval((String) trigger.get("id"));
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), query);

    assertThat(response).isNotEmpty();

    trigger = (Map<String, Object>) response.get("trigger");
    assertThat(trigger).isNotNull();

    assertThat(trigger.get("id")).isNotNull();
    assertThat(trigger.get("name")).isNotNull();

    Map<String, Object> condition = (Map<String, Object>) trigger.get("condition");
    assertThat(condition).isNotNull();
    assertThat(condition.get("webhookSource")).isEqualTo("CUSTOM");

    Map<String, Object> action = (Map<String, Object>) trigger.get("action");
    assertThat(action).isNotNull();
    assertThat(action.get("workflowId")).isEqualTo(savedWorkflow.getUuid());
    assertThat(action.get("workflowName")).isEqualTo(savedWorkflow.getName());

    List<Map<String, Object>> artifactSelections = (List<Map<String, Object>>) action.get("artifactSelections");
    assertThat(EmptyPredicate.isEmpty(artifactSelections)).isTrue();

    // DELETE
    mutation = getGraphQLQueryForTriggerDeletion(clientMutationId, application.getAppId(), triggerId);
    response = GraphQLRestUtils.executeGraphQLQuery(bearerToken, application.getAccountId(), mutation);

    assertThat(response).isNotEmpty();

    Map<String, Object> deleteTrigger = (Map<String, Object>) response.get("deleteTrigger");
    assertThat(deleteTrigger).isNotNull();

    assertThat(deleteTrigger.get("clientMutationId")).isEqualTo(clientMutationId);
  }

  private String getGraphQLQueryForTriggerCreation(
      String clientMutationId, String appId, String workflowId, String artifactSourceId, String serviceId) {
    return $GQL(/*
mutation {
createTrigger(input: {
clientMutationId: "%s",
applicationId: "%s",
name: "trigger1",
condition: {
conditionType: ON_WEBHOOK,
webhookConditionInput: {
webhookSourceType: CUSTOM
}
},
action: {
entityId: "%s",
executionType: WORKFLOW,
artifactSelections: {
artifactFilter: "filter",
artifactSelectionType: FROM_PAYLOAD_SOURCE,
artifactSourceId: "%s",
serviceId: "%s",
}
}
}) {
clientMutationId,
trigger {
id
}
}
}*/ clientMutationId, appId, workflowId, artifactSourceId, workflowId, serviceId);
  }

  private String getGraphQLQueryForTriggerUpdate(String clientMutationId, String triggerId, String triggerName,
      String appId, String workflowId, String artifactSourceId, String serviceId) {
    return $GQL(/*
mutation {
updateTrigger(input: {
triggerId: "%s",
clientMutationId: "%s",
applicationId: "%s",
name: "%s",
condition: {
conditionType: ON_WEBHOOK,
webhookConditionInput: {
webhookSourceType: CUSTOM
}
},
action: {
entityId: "%s",
executionType: WORKFLOW,
artifactSelections: {
artifactFilter: "filter",
artifactSelectionType: FROM_PAYLOAD_SOURCE,
artifactSourceId: "%s",
workflowId: "%s",
regex: false,
serviceId: "%s",
}
}
}) {
clientMutationId,
trigger {
id,
name
}
}
}*/ triggerId, clientMutationId, appId, triggerName, workflowId, artifactSourceId, workflowId, serviceId);
  }

  private String getGraphQLQueryForTriggerRetrieval(String triggerId) {
    return $GQL(/*
query {
trigger(triggerId: "%s") {
id,
name,
condition {
...on OnWebhook {
webhookSource
}
},
action {
...on WorkflowAction {
workflowId,
workflowName,
artifactSelections {
  serviceId,
  serviceName
}
}
}
}
}*/ triggerId);
  }

  private String getGraphQLQueryForTriggerDeletion(String clientMutationId, String applicationId, String triggerId) {
    return $GQL(/*
mutation {
deleteTrigger(input: {
clientMutationId: "%s",
applicationId: "%s",
triggerId: "%s"
}) {
clientMutationId
}
}*/ clientMutationId, applicationId, triggerId);
  }

  @After
  public void destroy() {
    workflowService.deleteWorkflow(application.getUuid(), savedWorkflow.getUuid());
  }
}
