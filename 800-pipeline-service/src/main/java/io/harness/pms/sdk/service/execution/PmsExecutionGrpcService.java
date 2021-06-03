package io.harness.pms.sdk.service.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.service.ExecutionSummaryResponse;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceImplBase;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.ExecutionSummaryUpdateUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PmsExecutionGrpcService extends PmsExecutionServiceImplBase {
  private static final String PIPELINE_MODULE_INFO_UPDATE_KEY = "moduleInfo.%s.%s";
  private static final String STAGE_MODULE_INFO_UPDATE_KEY = "layoutNodeMap.%s.moduleInfo.%s.%s";
  private static final String STAGE = StepOutcomeGroup.STAGE.name();
  private static final String PIPELINE = StepOutcomeGroup.PIPELINE.name();

  @Inject PmsExecutionSummaryRespository pmsExecutionSummaryRepository;
  @Inject private PMSPipelineService pmsPipelineService;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public void updateExecutionSummary(
      ExecutionSummaryUpdateRequest request, StreamObserver<ExecutionSummaryResponse> responseObserver) {
    NodeExecution nodeExecution = nodeExecutionService.get(request.getNodeExecutionId());
    updatePipelineInfoJson(request, nodeExecution);
    updateStageModuleInfo(request, nodeExecution);
    responseObserver.onNext(ExecutionSummaryResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  private void updatePipelineInfoJson(ExecutionSummaryUpdateRequest request, NodeExecution nodeExecution) {
    String moduleName = request.getModuleName();
    String planExecutionId = request.getPlanExecutionId();
    Document pipelineInfoDoc = RecastOrchestrationUtils.toDocumentFromJson(request.getPipelineModuleInfoJson());

    Update update = new Update();

    if (pipelineInfoDoc != null) {
      for (Map.Entry<String, Object> entry : pipelineInfoDoc.entrySet()) {
        String key = String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey());
        if (entry.getValue() != null && Collection.class.isAssignableFrom(entry.getValue().getClass())) {
          Collection<Object> values = (Collection<Object>) entry.getValue();
          update.addToSet(key).each(values);
        } else {
          if (entry.getValue() != null) {
            update.set(key, entry.getValue());
          }
        }
      }
    }
    ExecutionSummaryUpdateUtils.addPipelineUpdateCriteria(update, planExecutionId, nodeExecution);
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  private void updateStageModuleInfo(ExecutionSummaryUpdateRequest request, NodeExecution nodeExecution) {
    String stageUuid = request.getNodeUuid();
    String moduleName = request.getModuleName();
    String stageInfo = request.getNodeModuleInfoJson();
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(nodeExecution.getStatus());
    String planExecutionId = request.getPlanExecutionId();
    if (EmptyPredicate.isEmpty(stageUuid)) {
      return;
    }
    Document stageInfoDoc = RecastOrchestrationUtils.toDocumentFromJson(stageInfo);

    Update update = new Update();
    if (stageInfoDoc != null) {
      for (Map.Entry<String, Object> entry : stageInfoDoc.entrySet()) {
        String key = String.format(STAGE_MODULE_INFO_UPDATE_KEY, stageUuid, moduleName, entry.getKey());
        if (entry.getValue() != null && Collection.class.isAssignableFrom(entry.getValue().getClass())) {
          Collection<Object> values = (Collection<Object>) entry.getValue();
          update.addToSet(key).each(values);
        } else {
          if (entry.getValue() != null) {
            update.set(key, entry.getValue());
          }
        }
      }
    }
    ExecutionSummaryUpdateUtils.addStageUpdateCriteria(update, planExecutionId, nodeExecution);
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
