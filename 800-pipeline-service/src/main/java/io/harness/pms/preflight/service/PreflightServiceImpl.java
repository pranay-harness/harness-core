package io.harness.pms.preflight.service;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.merger.helpers.MergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.connector.ConnectorPreflightHandler;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.entity.PreFlightEntity.PreFlightEntityKeys;
import io.harness.pms.preflight.handler.AsyncPreFlightHandler;
import io.harness.pms.preflight.mapper.PreFlightMapper;
import io.harness.pms.rbac.validator.PipelineRbacService;
import io.harness.repositories.preflight.PreFlightRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PreflightServiceImpl implements PreflightService {
  private static final ExecutorService executorService = Executors.newFixedThreadPool(1);

  @Inject PreFlightRepository preFlightRepository;
  @Inject ConnectorPreflightHandler connectorPreflightHandler;
  @Inject PMSPipelineService pmsPipelineService;
  @Inject PipelineSetupUsageHelper pipelineSetupUsageHelper;
  @Inject PipelineRbacService pipelineRbacServiceImpl;
  @Inject PreflightService preflightService;

  public void updateStatus(String id, PreFlightStatus status, PreFlightEntityErrorInfo errorInfo) {
    Criteria criteria = Criteria.where(PreFlightEntityKeys.uuid).is(id);
    Update update = new Update();
    update.set(PreFlightEntityKeys.preFlightStatus, status);
    if (errorInfo != null) {
      update.set(PreFlightEntityKeys.errorInfo, errorInfo);
    }
    if (status == PreFlightStatus.FAILURE || status == PreFlightStatus.SUCCESS) {
      update.set(
          PreFlightEntityKeys.validUntil, Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(5)).toInstant()));
    }
    preFlightRepository.update(criteria, update);
  }

  @Override
  public List<ConnectorCheckResponse> updateConnectorCheckResponses(String accountId, String orgId, String projectId,
      String preflightEntityId, Map<String, Object> fqnToObjectMapMergedYaml, List<EntityDetail> connectorUsages) {
    List<ConnectorCheckResponse> connectorCheckResponses =
        connectorPreflightHandler.getConnectorCheckResponsesForReferredConnectors(
            accountId, orgId, projectId, fqnToObjectMapMergedYaml, connectorUsages);
    if (!EmptyPredicate.isEmpty(connectorCheckResponses)) {
      Criteria criteria = Criteria.where(PreFlightEntityKeys.uuid).is(preflightEntityId);
      Update update = new Update();
      update.set(PreFlightEntityKeys.connectorCheckResponse, connectorCheckResponses);
      preFlightRepository.update(criteria, update);
    }
    return connectorCheckResponses;
  }

  @Override
  public PreFlightEntity saveEmptyPreflightEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineYaml, List<EntityDetail> entityDetails) {
    PreFlightEntity preFlightEntity =
        PreFlightMapper.toEmptyEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);
    List<EntityDetail> connectorUsages = entityDetails.stream()
                                             .filter(entityDetail -> entityDetail.getType() == EntityType.CONNECTORS)
                                             .collect(Collectors.toList());
    List<ConnectorCheckResponse> connectorTemplates =
        connectorPreflightHandler.getConnectorCheckResponseTemplate(connectorUsages);
    preFlightEntity.setConnectorCheckResponse(connectorTemplates);
    return preFlightRepository.save(preFlightEntity);
  }

  @Override
  public PreFlightDTO getPreflightCheckResponse(String preflightCheckId) {
    Optional<PreFlightEntity> optionalPreFlightEntity = preFlightRepository.findById(preflightCheckId);
    if (!optionalPreFlightEntity.isPresent()) {
      throw new InvalidRequestException("Could not find pre flight check data corresponding to id:" + preflightCheckId);
    }
    PreFlightEntity preFlightEntity = optionalPreFlightEntity.get();
    return PreFlightMapper.toPreFlightDTO(preFlightEntity);
  }

  @Override
  public String startPreflightCheck(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml)
      throws IOException {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml = MergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, false);
    }
    List<EntityDetail> entityDetails = pipelineSetupUsageHelper.getReferencesOfPipeline(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml, null);
    pipelineRbacServiceImpl.validateStaticallyReferredEntities(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml, entityDetails);
    PreFlightEntity preFlightEntitySaved = preflightService.saveEmptyPreflightEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml, entityDetails);

    executorService.submit(AsyncPreFlightHandler.builder()
                               .entity(preFlightEntitySaved)
                               .entityDetails(entityDetails)
                               .preflightService(this)
                               .build());
    return preFlightEntitySaved.getUuid();
  }
}
