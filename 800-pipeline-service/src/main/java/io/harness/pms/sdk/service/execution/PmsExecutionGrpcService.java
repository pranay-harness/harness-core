package io.harness.pms.sdk.service.execution;

import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.service.ExecutionSummaryCreateRequest;
import io.harness.pms.contracts.service.ExecutionSummaryResponse;
import io.harness.pms.contracts.service.ExecutionSummaryUpdateRequest;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceImplBase;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.pms.pipeline.mappers.GraphLayoutDtoMapper;
import io.harness.pms.pipeline.resource.GraphLayoutNodeDTO;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

  @Override
  public void updateExecutionSummary(
      ExecutionSummaryUpdateRequest request, StreamObserver<ExecutionSummaryResponse> responseObserver) {
    updatePipelineInfoJson(request);
    updateStageModuleInfo(request);
    responseObserver.onNext(ExecutionSummaryResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void createExecutionSummary(
      ExecutionSummaryCreateRequest request, StreamObserver<ExecutionSummaryResponse> responseObserver) {
    Optional<PipelineEntity> pipelineEntity = pmsPipelineService.get(
        request.getAccountId(), request.getOrgId(), request.getProjectId(), request.getPipelineId(), false);
    if (!pipelineEntity.isPresent()) {
      return;
    }
    Map<String, GraphLayoutNode> layoutNodeMap = pipelineEntity.get().getLayoutNodeMap();
    String startingNodeId = pipelineEntity.get().getStartingNodeID();
    Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap = new HashMap<>();
    for (Map.Entry<String, GraphLayoutNode> entry : layoutNodeMap.entrySet()) {
      layoutNodeDTOMap.put(entry.getKey(), GraphLayoutDtoMapper.toDto(entry.getValue()));
    }
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        PipelineExecutionSummaryEntity.builder()
            .layoutNodeMap(layoutNodeDTOMap)
            .pipelineIdentifier(request.getPipelineId())
            .startingNodeId(pipelineEntity.get().getStartingNodeID())
            .planExecutionId(request.getPlanExecutionId())
            .name(request.getName())
            .inputSetYaml(request.getInputSetYaml())
            .status(ExecutionStatus.NOT_STARTED)
            .startTs((long) request.getStartTs())
            .endTs((long) request.getEndTs())
            .startingNodeId(startingNodeId)
            .build();
    pmsExecutionSummaryRepository.save(pipelineExecutionSummaryEntity);
    responseObserver.onNext(ExecutionSummaryResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  private void updatePipelineInfoJson(ExecutionSummaryUpdateRequest request) {
    String moduleName = request.getModuleName();
    String planExecutionId = request.getPlanExecutionId();
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(request.getStatus());
    Map<String, Object> fieldToValues = JsonUtils.asMap(request.getPipelineModuleInfoJson());
    Update update = new Update();

    for (Map.Entry<String, Object> entry : fieldToValues.entrySet()) {
      if (Collection.class.isAssignableFrom(entry.getValue().getClass())) {
        Collection<Object> values = (Collection<Object>) entry.getValue();
        for (Object value : values) {
          update.addToSet(String.format(PIPELINE_MODULE_INFO_UPDATE_KEY, moduleName, entry.getKey()), value);
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
    if (request.getNodeType().equals("pipeline")) {
      update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.status, status);
      if (ExecutionStatus.isTerminal(status)) {
        update.set(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.endTs, request.getEndTs());
      }
    }
    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }

  private void updateStageModuleInfo(ExecutionSummaryUpdateRequest request) {
    String stageUuid = request.getStageUuid();
    String moduleName = request.getModuleName();
    String stageInfo = request.getStageModuleInfoJson();
    ExecutionStatus status = ExecutionStatus.getExecutionStatus(request.getStatus());
    String planExecutionId = request.getPlanExecutionId();
    if (stageUuid == null) {
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
    if (request.getNodeType().equals("stage")) {
      update.set(
          PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.layoutNodeMap + "." + stageUuid + ".status", status);
    }

    Criteria criteria =
        Criteria.where(PipelineExecutionSummaryEntity.PlanExecutionSummaryKeys.planExecutionId).is(planExecutionId);
    Query query = new Query(criteria);
    pmsExecutionSummaryRepository.update(query, update);
  }
}
