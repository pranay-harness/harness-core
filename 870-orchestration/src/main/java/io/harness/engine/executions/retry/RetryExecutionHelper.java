package io.harness.engine.executions.retry;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.exception.InvalidRequestException;
import io.harness.plan.IdentityPlanNode;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class RetryExecutionHelper {
  private static final String LAST_STAGE_IDENTIFIER = "last_stage_identifier";
  private final NodeExecutionService nodeExecutionService;
  private final PlanExecutionMetadataService planExecutionMetadataService;

  public List<String> fetchOnlyFailedStages(List<RetryStageInfo> info, List<String> retryStagesIdentifier) {
    List<String> onlyFailedStage = new ArrayList<>();
    for (int i = 0; i < info.size(); i++) {
      RetryStageInfo stageInfo = info.get(i);
      String stageIdentifier = stageInfo.getIdentifier();
      if (!retryStagesIdentifier.contains(stageIdentifier)) {
        throw new InvalidRequestException("Run only failed stages is applicable only for failed parallel group stages");
      }
      if (isFailedStatus(stageInfo.getStatus())) {
        onlyFailedStage.add(stageInfo.getIdentifier());
      }
    }
    if (onlyFailedStage.size() == 0) {
      throw new InvalidRequestException("No failed stage found in parallel group");
    }
    return onlyFailedStage;
  }
  public List<String> fetchOnlyFailedStages(String previousExecutionId, List<String> retryStagesIdentifier) {
    RetryInfo retryInfo = getRetryInfo(getStageDetails(previousExecutionId));
    if (retryInfo != null) {
      List<RetryStageInfo> info = retryInfo.getGroups().get(retryInfo.getGroups().size() - 1).getInfo();
      return fetchOnlyFailedStages(info, retryStagesIdentifier);
    }
    throw new InvalidRequestException("Pipeline is updated, cannot resume");
  }

  public boolean isFailedStatus(ExecutionStatus status) {
    if (status.equals(ExecutionStatus.ABORTED) || status.equals(ExecutionStatus.FAILED)
        || status.equals(ExecutionStatus.EXPIRED) || status.equals(ExecutionStatus.APPROVAL_REJECTED)
        || status.equals(ExecutionStatus.APPROVALREJECTED)) {
      return true;
    }
    return false;
  }

  public boolean validateRetry(String updatedYaml, String executedYaml) {
    // compare fqn
    if (isEmpty(updatedYaml) || isEmpty(executedYaml)) {
      return false;
    }

    YamlConfig updatedConfig = new YamlConfig(updatedYaml);
    YamlConfig executedConfig = new YamlConfig(executedYaml);

    Map<FQN, Object> fqnToValueMapUpdatedYaml = updatedConfig.getFqnToValueMap();
    Map<FQN, Object> fqnToValueMapExecutedYaml = executedConfig.getFqnToValueMap();

    List<String> updateStageIdentifierList = new ArrayList<>();
    for (FQN fqn : fqnToValueMapUpdatedYaml.keySet()) {
      if (fqn.isStageIdentifier()) {
        updateStageIdentifierList.add(fqn.display());
      }
    }

    List<String> executedStageIdentifierList = new ArrayList<>();
    for (FQN fqn : fqnToValueMapExecutedYaml.keySet()) {
      if (fqn.isStageIdentifier()) {
        executedStageIdentifierList.add(fqn.display());
      }
    }

    if (!updateStageIdentifierList.equals(executedStageIdentifierList)) {
      return false;
    }
    return true;
  }

  public RetryInfo getRetryStages(
      String updatedYaml, String executedYaml, String planExecutionId, String pipelineIdentifier) {
    if (isEmpty(planExecutionId)) {
      return null;
    }
    boolean isResumable = validateRetry(updatedYaml, executedYaml);
    if (!isResumable) {
      return RetryInfo.builder().isResumable(isResumable).errorMessage("Pipeline is updated, cannot retry").build();
    }
    List<RetryStageInfo> stageDetails = getStageDetails(planExecutionId);

    return getRetryInfo(stageDetails);
  }

  public RetryInfo getRetryInfo(List<RetryStageInfo> stageDetails) {
    HashMap<String, List<RetryStageInfo>> mapNextIdWithStageInfo = new LinkedHashMap<>();
    for (RetryStageInfo stageDetail : stageDetails) {
      String nextId = stageDetail.getNextId();
      if (isEmpty(nextId)) {
        nextId = LAST_STAGE_IDENTIFIER;
      }
      List<RetryStageInfo> stageList = mapNextIdWithStageInfo.getOrDefault(nextId, new ArrayList<>());
      stageList.add(stageDetail);
      mapNextIdWithStageInfo.put(nextId, stageList);
    }
    List<RetryGroup> retryGroupList = new ArrayList<>();
    for (Map.Entry<String, List<RetryStageInfo>> entry : mapNextIdWithStageInfo.entrySet()) {
      retryGroupList.add(RetryGroup.builder().info(entry.getValue()).build());
    }
    return RetryInfo.builder().isResumable(true).groups(retryGroupList).build();
  }

  public List<RetryStageInfo> getStageDetails(String planExecutionId) {
    return nodeExecutionService.getStageDetailFromPlanExecutionId(planExecutionId);
  }

  public String getYamlFromExecutionId(String planExecutionId) {
    return planExecutionMetadataService.getYamlFromPlanExecutionId(planExecutionId);
  }

  public String retryProcessedYaml(String previousProcessedYaml, String currentProcessedYaml, List<String> retryStages,
      List<String> uuidForSkipNode) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    JsonNode previousRootJsonNode = mapper.readTree(previousProcessedYaml);
    JsonNode currentRootJsonNode = mapper.readTree(currentProcessedYaml);

    if (previousRootJsonNode == null || currentRootJsonNode == null) {
      return currentProcessedYaml;
    }
    int stageCounter = 0;
    JsonNode stagesNode = previousRootJsonNode.get("pipeline").get("stages");
    for (JsonNode stage : stagesNode) {
      // stage is not a part of parallel group
      if (stage.get("stage") != null) {
        // if the stage does not belongs to the retry stages and is to be skipped, copy the stage node from the
        // previous processed yaml
        JsonNode previousExecutionStageNodeJson = stage.get("stage");
        String stageIdentifier = previousExecutionStageNodeJson.get("identifier").textValue();
        if (!retryStages.contains(stageIdentifier)) {
          traverseUuid(stage, uuidForSkipNode);
          ((ArrayNode) currentRootJsonNode.get("pipeline").get("stages")).set(stageCounter, stage);
          stageCounter = stageCounter + 1;
        } else {
          // need to copy only the uuid of the stage to be retry.
          JsonNode currentResumableStagejsonNode =
              currentRootJsonNode.get("pipeline").get("stages").get(stageCounter).get("stage");
          ((ObjectNode) currentResumableStagejsonNode).set("__uuid", previousExecutionStageNodeJson.get("__uuid"));
          // here onwards we need to retry the pipeline, no further copy of nodes required
          break;
        }
      } else {
        // parallel group
        if (!isRetryStagesInParallelStages(stage.get("parallel"), retryStages)) {
          traverseUuid(stage, uuidForSkipNode);
          // if the parallel group does not contain the retry stages, copy the whole parallel node
          ((ArrayNode) currentRootJsonNode.get("pipeline").get("stages")).set(stageCounter, stage);
          stageCounter = stageCounter + 1;
        } else {
          // replace only those stages that needs to be skipped
          ((ArrayNode) currentRootJsonNode.get("pipeline").get("stages"))
              .set(stageCounter,
                  replaceStagesInParallelGroup(stage.get("parallel"), retryStages,
                      currentRootJsonNode.get("pipeline").get("stages").get(stageCounter), uuidForSkipNode));
          break;
        }
      }
    }
    return currentRootJsonNode.toString();
  }

  private JsonNode replaceStagesInParallelGroup(JsonNode parallelStage, List<String> retryStages,
      JsonNode currentParallelStageNode, List<String> uuidForSkipNode) {
    int stageCounter = 0;
    for (JsonNode stageNode : parallelStage) {
      String stageIdentifier = stageNode.get("stage").get("identifier").textValue();
      if (!retryStages.contains(stageIdentifier)) {
        traverseUuid(stageNode, uuidForSkipNode);
        ((ArrayNode) currentParallelStageNode.get("parallel")).set(stageCounter, stageNode);
      } else {
        // replace only the uuid of the retry parallel stage
        JsonNode currentResumableStagejsonNode =
            currentParallelStageNode.get("parallel").get(stageCounter).get("stage");
        ((ObjectNode) currentResumableStagejsonNode).set("__uuid", stageNode.get("stage").get("__uuid"));
      }
      stageCounter++;
    }
    return currentParallelStageNode;
  }

  private boolean isRetryStagesInParallelStages(JsonNode parallelStage, List<String> retryStages) {
    for (JsonNode stageNode : parallelStage) {
      String stageIdentifier = stageNode.get("stage").get("identifier").textValue();
      if (retryStages.contains(stageIdentifier)) {
        return true;
      }
    }
    return false;
  }

  private void traverseUuid(JsonNode node, List<String> uuidForSkipNode) {
    if (node == null) {
      return;
    }
    if (node.isObject()) {
      injectUuidInObject(node, uuidForSkipNode);
    } else if (node.isArray()) {
      injectUuidInArray(node, uuidForSkipNode);
    }
  }

  private void injectUuidInArray(JsonNode node, List<String> uuidForSkipNode) {
    ArrayNode arrayNode = (ArrayNode) node;
    for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext();) {
      traverseUuid(it.next(), uuidForSkipNode);
    }
  }

  private void injectUuidInObject(JsonNode node, List<String> uuidForSkipNode) {
    ObjectNode objectNode = (ObjectNode) node;
    if (node.get("__uuid") != null) {
      uuidForSkipNode.add(node.get("__uuid").textValue());
    }
    for (Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext();) {
      Map.Entry<String, JsonNode> field = it.next();
      traverseUuid(field.getValue(), uuidForSkipNode);
    }
  }

  public Plan transformPlan(Plan plan, List<String> uuidForSkipNode, String previousExecutionId) {
    List<Node> planNodes = plan.getPlanNodes();
    /*
    Mapping planNode.uuid with uuid in NodeExecution
    planNode.uuid -> nodeExecution.uuid
     */
    List<Node> updatedPlanNodes = new ArrayList<>();
    Map<String, String> nodeUuidToNodeExecutionUuid =
        nodeExecutionService.fetchNodeExecutionUuidFromNodeUuidsAndPlanExecutionId(
            uuidForSkipNode, previousExecutionId);

    /*
    IdentityNode
    Convert the planNode to Identity Node whose uuids are there in map uuidForSkipNode
     */
    planNodes.stream()
        .filter(node -> nodeUuidToNodeExecutionUuid.containsKey(node.getUuid()))
        .forEach(node1
            -> updatedPlanNodes.add(
                mapPlanNodeToIdentityNode(node1, nodeUuidToNodeExecutionUuid.get(node1.getUuid()))));

    /*
    Copy of Node whose uuid is not in uuidForSkipNode
     */
    planNodes.stream()
        .filter(node -> !nodeUuidToNodeExecutionUuid.containsKey(node.getUuid()))
        .forEach(node1 -> updatedPlanNodes.add(node1));

    return Plan.builder()
        .uuid(plan.getUuid())
        .planNodes(updatedPlanNodes)
        .startingNodeId(plan.getStartingNodeId())
        .setupAbstractions(plan.getSetupAbstractions())
        .graphLayoutInfo(plan.getGraphLayoutInfo())
        .validUntil(plan.getValidUntil())
        .createdAt(plan.getCreatedAt())
        .version(plan.getVersion())
        .valid(plan.isValid())
        .errorResponse(plan.getErrorResponse())
        .build();
  }

  private IdentityPlanNode mapPlanNodeToIdentityNode(Node node, String originalNodeExecutionUuid) {
    return IdentityPlanNode.builder()
        .uuid(node.getUuid())
        .name(node.getName())
        .identifier(node.getIdentifier())
        .group(node.getGroup())
        .originalNodeExecutionId(originalNodeExecutionUuid)
        .build();
  }
}
