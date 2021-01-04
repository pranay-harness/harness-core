package io.harness.pms.sdk.service.execution;

import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.service.ExecutionSummaryResponse;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceImplBase;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionErrorInfo;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
public class PmsExecutionGrpcService extends PmsExecutionServiceImplBase {
  private static final String PIPELINE_MODULE_INFO_UPDATE_KEY = "moduleInfo.%s.%s";
  private static final String STAGE_MODULE_INFO_UPDATE_KEY = "layoutNodeMap.%s.moduleInfo.%s.%s";

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
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(nodeExecution.getStatus());
    Map<String, Object> fieldToValues = JsonUtils.asMap(request.getPipelineModuleInfoJson());
    Update update = new Update();

    for (Map.Entry<String, Object> entry : fieldToValues.entrySet()) {
      if (Collection.class.isAssignableFrom(entry.getValue().getClass())) {
        Collection<Object> values = (Collection<Object>) entry.getValue();
        for (Object value : values) {
          update.addToSet(String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey()), value);
        }
        if (values.isEmpty()) {
          update.addToSet(String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey()), "");
        }
      } else {
        if (entry.getValue() instanceof String) {
          update.set(
              String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey()), entry.getValue().toString());
        } else {
          update.set(String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey()),
              Document.parse(JsonUtils.asJson(entry.getValue())));
        }
      }
    }
    if (Objects.equals(nodeExecution.getNode().getGroup(), "PIPELINE")) {
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.status, status);
      if (ExecutionStatus.isTerminal(status)) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, nodeExecution.getEndTs());
      }
      if (status == ExecutionStatus.FAILED) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.executionErrorInfo,
            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
    }
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
    Map<String, Object> fieldToValues = JsonUtils.asMap(stageInfo);
    Update update = new Update();
    for (Map.Entry<String, Object> entry : fieldToValues.entrySet()) {
      if (Collection.class.isAssignableFrom(entry.getValue().getClass())) {
        Collection<Object> values = (Collection<Object>) entry.getValue();
        for (Object value : values) {
          update.addToSet(String.format(STAGE_MODULE_INFO_UPDATE_KEY, stageUuid, moduleName, entry.getKey()), value);
        }
        if (values.isEmpty()) {
          update.addToSet(String.format(STAGE_MODULE_INFO_UPDATE_KEY, stageUuid, moduleName, entry.getKey()), "");
        }
      } else {
        if (entry.getValue() instanceof String) {
          update.set(String.format(STAGE_MODULE_INFO_UPDATE_KEY, stageUuid, moduleName, entry.getKey()),
              entry.getValue().toString());
        } else {
          update.set(String.format(STAGE_MODULE_INFO_UPDATE_KEY, stageUuid, moduleName, entry.getKey()),
              Document.parse(JsonUtils.asJson(entry.getValue())));
        }
      }
    }
    if (Objects.equals(nodeExecution.getNode().getGroup(), "STAGE")) {
      update.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".status", status);
    }

    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
