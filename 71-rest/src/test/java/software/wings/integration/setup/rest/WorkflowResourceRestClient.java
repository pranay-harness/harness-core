package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.RollingOrchestrationWorkflow.RollingOrchestrationWorkflowBuilder.aRollingOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.integration.SeedData.randomText;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_BASIC_WORKFLOW_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_BASIC_WORKFLOW_NAME;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_ROLLING_WORKFLOW_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_ROLLING_WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStepType;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.integration.UserResourceRestClient;
import software.wings.sm.StateType;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Singleton
public class WorkflowResourceRestClient {
  @Inject private UserResourceRestClient userResourceRestClient;
  @Inject private AppResourceRestClient appResourceRestClient;
  @Inject private ServiceResourceRestClient serviceResourceRestClient;
  @Inject private EnvResourceRestClient envResourceRestClient;

  private ConcurrentHashMap<String, Workflow> cachedEntity = new ConcurrentHashMap<>();

  public Workflow getSeedBasicWorkflow(Client client) {
    return cachedEntity.computeIfAbsent(SEED_BASIC_WORKFLOW_KEY,
        key -> fetchOrCreateWorkflow(client, SEED_BASIC_WORKFLOW_NAME, aBasicOrchestrationWorkflow().build()));
  }

  public Workflow getSeedRollingWorkflow(Client client) {
    return cachedEntity.computeIfAbsent(SEED_ROLLING_WORKFLOW_KEY,
        key -> fetchOrCreateWorkflow(client, SEED_ROLLING_WORKFLOW_NAME, aRollingOrchestrationWorkflow().build()));
  }

  public Workflow getSeedBuildWorkflow(Client client) {
    return cachedEntity.computeIfAbsent(SEED_ROLLING_WORKFLOW_KEY,
        key -> fetchOrCreateWorkflow(client, SEED_ROLLING_WORKFLOW_NAME, aRollingOrchestrationWorkflow().build()));
  }

  private Workflow fetchOrCreateWorkflow(
      Client client, String workflowName, OrchestrationWorkflow orchestrationWorkflow) {
    String appId = appResourceRestClient.getSeedApplication(client).getUuid();
    Workflow workflow = getWorkflowByName(client, userResourceRestClient.getUserToken(client), appId, workflowName);
    if (workflow == null) {
      InfrastructureMapping infraMapping = envResourceRestClient.getSeedFakeHostsDcInfra(client);

      workflow = aWorkflow()
                     .withName(workflowName)
                     .withAppId(appId)
                     .withWorkflowType(WorkflowType.ORCHESTRATION)
                     .withDescription(randomText(40))
                     .withServiceId(infraMapping.getServiceId())
                     .withEnvId(infraMapping.getEnvId())
                     .withInfraMappingId(infraMapping.getUuid())
                     .withOrchestrationWorkflow(orchestrationWorkflow)
                     .build();
      workflow = createWorkflow(client, userResourceRestClient.getUserToken(client), appId, workflow);
      updateWithHttpSteps(client, userResourceRestClient.getUserToken(client), appId, workflow);
    }
    return workflow;
  }

  public Workflow getWorkflowByName(Client client, String userToken, String appId, String name) {
    WebTarget target = client.target(API_BASE + "/workflows/?appId=" + appId + "&name=" + URLEncoder.encode(name));

    RestResponse<PageResponse<Workflow>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Workflow>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Workflow getWorkflow(Client client, String userToken, String appId, String workflowId) {
    WebTarget target = client.target(API_BASE + "/workflows/" + workflowId + "?appId=" + appId);

    RestResponse<Workflow> response = userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
                                          .get(new GenericType<RestResponse<Workflow>>() {});
    assertThat(response.getResource())
        .isNotNull()
        .isInstanceOf(Workflow.class)
        .hasFieldOrProperty("uuid")
        .hasFieldOrPropertyWithValue("uuid", workflowId);
    return response.getResource();
  }

  public Workflow createWorkflow(Client client, String userToken, String appId, Workflow workflow) {
    WebTarget target = client.target(API_BASE + "/workflows/?appId=" + appId);

    RestResponse<Service> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(workflow, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(Service.class);
    String workflowId = response.getResource().getUuid();
    assertThat(workflowId).isNotEmpty();
    Workflow fetched = getWorkflow(client, userToken, appId, workflowId);
    assertThat(fetched)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", workflowId)
        .hasFieldOrPropertyWithValue("name", workflow.getName());
    return fetched;
  }

  public Workflow updateWorkflow(Client client, String userToken, String appId, Workflow workflow) {
    WebTarget target = client.target(API_BASE + "/workflows/" + workflow.getUuid() + "?appId=" + appId);

    RestResponse<Service> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .put(entity(workflow, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(Service.class);
    String workflowId = response.getResource().getUuid();
    assertThat(workflowId).isNotEmpty().isEqualTo(workflow.getUuid());
    Workflow fetched = getWorkflow(client, userToken, appId, workflowId);
    assertThat(fetched)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", workflowId)
        .hasFieldOrPropertyWithValue("name", workflow.getName());
    return fetched;
  }

  public WorkflowPhase updateWorkflowPhase(
      Client client, String userToken, String appId, String workflowId, WorkflowPhase workflowPhase) {
    return updateWorkflowPhase(client, userToken, appId, workflowId, workflowPhase.getUuid(), workflowPhase, false);
  }

  public WorkflowPhase updateRollbackPhase(Client client, String userToken, String appId, String workflowId,
      String workflowPhaseId, WorkflowPhase workflowPhase) {
    return updateWorkflowPhase(client, userToken, appId, workflowId, workflowPhaseId, workflowPhase, true);
  }

  private WorkflowPhase updateWorkflowPhase(Client client, String userToken, String appId, String workflowId,
      String workflowPhaseId, WorkflowPhase workflowPhase, boolean rollback) {
    WebTarget target = client.target(API_BASE + "/workflows/" + workflowId + "/phases/" + workflowPhaseId
        + (rollback ? "/rollback" : "") + "?appId=" + appId);

    RestResponse<WorkflowPhase> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .put(entity(workflowPhase, APPLICATION_JSON), new GenericType<RestResponse<WorkflowPhase>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(WorkflowPhase.class);
    return response.getResource();
  }

  private void updateWithHttpSteps(Client client, String userToken, String appId, Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow == null) {
      return;
    }
    if (!(orchestrationWorkflow instanceof CanaryOrchestrationWorkflow)) {
      return;
    }

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    canaryOrchestrationWorkflow.getWorkflowPhases().forEach(workflowPhase -> {
      updateWithHttpSteps(client, userResourceRestClient.getUserToken(client), appId, workflow, workflowPhase.getUuid(),
          workflowPhase, false);
    });
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    rollbackWorkflowPhaseIdMap.keySet().forEach(workflowPhaseId -> {
      updateWithHttpSteps(client, userResourceRestClient.getUserToken(client), appId, workflow, workflowPhaseId,
          rollbackWorkflowPhaseIdMap.get(workflowPhaseId), true);
    });
  }

  private void updateWithHttpSteps(Client client, String userToken, String appId, Workflow workflow,
      String workflowPhaseId, WorkflowPhase workflowPhase, boolean rollback) {
    workflowPhase.getPhaseSteps().forEach(phaseStep -> {
      if (phaseStep.getPhaseStepType() == PhaseStepType.INFRASTRUCTURE_NODE) {
        return;
      }
      List<GraphNode> steps = phaseStep.getSteps();
      if (steps != null) {
        ArrayList<GraphNode> newSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
          newSteps.add(aGraphNode()
                           .withType(StateType.HTTP.name())
                           .withRollback(rollback)
                           .withName("Http-" + System.currentTimeMillis() + "-" + i)
                           .withProperties(ImmutableMap.of("url", "http://google.com?h=${host.name}", "method", "GET"))
                           .build());
        }
        phaseStep.setSteps(newSteps);

        updateWorkflowPhase(client, userToken, appId, workflow.getUuid(), workflowPhaseId, workflowPhase, rollback);
      }
    });
  }
}
