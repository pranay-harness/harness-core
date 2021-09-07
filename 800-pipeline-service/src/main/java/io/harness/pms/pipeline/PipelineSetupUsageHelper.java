package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.remote.client.NGRestUtils.getResponseWithRetry;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO.EntityReferredByPipelineDetailProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO.PipelineDetailType;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetailType;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.rbac.InternalReferredEntityExtractor;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.IdentifierRefHelper;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PipelineSetupUsageHelper implements PipelineActionObserver {
  @Inject @Named(EventsFrameworkConstants.SETUP_USAGE) private Producer eventProducer;
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  @Inject private EntitySetupUsageClient entitySetupUsageClient;
  @Inject private InternalReferredEntityExtractor internalReferredEntityExtractor;
  private static final int PAGE = 0;
  private static final int SIZE = 100;

  /**
   * Performs the following:
   * - queries the entitySetupUsage framework to get all entities referenced in the given pipeline yaml. (static, inputs
   * and runtimeExpression)
   * - extracts the value of a runtime input using fqn from the given pipeline yaml
   * - does not resolve runtimeExpressions as they can only be resolved during execution.
   * - can filter out resources of given entityType too. If entityType is null, it gives all resources.
   *
   * @param accountIdentifier - accountIdentifier of the pipeline
   * @param orgIdentifier -  orgIdentifier of the pipeline
   * @param projectIdentifier - projectIdentifier of the pipeline
   * @param pipelineId - pipelineIdentifier
   * @param pipelineYaml - merged pipeline yaml.
   * @param entityType - returns response of given entity type referred in the pipeline.
   */
  public List<EntityDetail> getReferencesOfPipeline(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String pipelineId, String pipelineYaml, EntityType entityType) {
    Map<String, Object> fqnToObjectMapMergedYaml = new HashMap<>();
    try {
      Map<FQN, Object> fqnObjectMap =
          FQNMapGenerator.generateFQNMap(YamlUtils.readTree(pipelineYaml).getNode().getCurrJsonNode());
      fqnObjectMap.keySet().forEach(fqn -> fqnToObjectMapMergedYaml.put(fqn.getExpressionFqn(), fqnObjectMap.get(fqn)));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid merged pipeline yaml");
    }

    List<EntitySetupUsageDTO> allReferredUsages =
        getResponseWithRetry(entitySetupUsageClient.listAllReferredUsages(PAGE, SIZE, accountIdentifier,
                                 FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
                                     accountIdentifier, orgIdentifier, projectIdentifier, pipelineId),
                                 entityType, null),
            "Could not extract setup usage of pipeline with id " + pipelineId + " after {} attempts.");
    List<EntityDetail> entityDetails = new ArrayList<>();
    for (EntitySetupUsageDTO referredUsage : allReferredUsages) {
      IdentifierRef ref = (IdentifierRef) referredUsage.getReferredEntity().getEntityRef();
      Map<String, String> metadata = ref.getMetadata();
      if (metadata == null) {
        continue;
      }
      String fqn = metadata.get(PreFlightCheckMetadata.FQN);

      if (!metadata.containsKey(PreFlightCheckMetadata.EXPRESSION)) {
        entityDetails.add(referredUsage.getReferredEntity());
      } else if (fqnToObjectMapMergedYaml.containsKey(fqn)) {
        String finalValue = ((TextNode) fqnToObjectMapMergedYaml.get(fqn)).asText();
        if (NGExpressionUtils.isRuntimeOrExpressionField(finalValue)) {
          continue;
        }
        if (ParameterField.containsInputSetValidator(finalValue)) {
          finalValue = ParameterField.getValueFromParameterFieldWithInputSetValidator(finalValue);
        }
        IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
            finalValue, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
        entityDetails.add(EntityDetail.builder()
                              .name(referredUsage.getReferredEntity().getName())
                              .type(referredUsage.getReferredEntity().getType())
                              .entityRef(identifierRef)
                              .build());
      }
    }
    entityDetails.addAll(internalReferredEntityExtractor.extractInternalEntities(accountIdentifier, entityDetails));
    return entityDetails;
  }

  public void publishSetupUsageEvent(PipelineEntity pipelineEntity, List<EntityDetailProtoDTO> referredEntities) {
    if (EmptyPredicate.isEmpty(referredEntities)) {
      return;
    }
    EntityDetailProtoDTO pipelineDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(pipelineEntity.getAccountId(),
                pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
                pipelineEntity.getIdentifier()))
            .setType(EntityTypeProtoEnum.PIPELINES)
            .setName(pipelineEntity.getName())
            .build();

    Map<String, List<EntityDetailProtoDTO>> referredEntityTypeToReferredEntities = new HashMap<>();
    for (EntityDetailProtoDTO entityDetailProtoDTO : referredEntities) {
      List<EntityDetailProtoDTO> entityDetailProtoDTOS =
          referredEntityTypeToReferredEntities.getOrDefault(entityDetailProtoDTO.getType().name(), new ArrayList<>());
      entityDetailProtoDTOS.add(entityDetailProtoDTO);
      referredEntityTypeToReferredEntities.put(entityDetailProtoDTO.getType().name(), entityDetailProtoDTOS);
    }

    for (Map.Entry<String, List<EntityDetailProtoDTO>> entry : referredEntityTypeToReferredEntities.entrySet()) {
      List<EntityDetailProtoDTO> entityDetailProtoDTOs = entry.getValue();
      List<EntityDetailWithSetupUsageDetailProtoDTO> entityDetailWithSetupUsageDetailProtoDTOS =
          convertToReferredEntityWithSetupUsageDetail(entityDetailProtoDTOs,
              Objects.requireNonNull(SetupUsageDetailType.getTypeFromEntityTypeProtoEnumName(entry.getKey())).name());
      EntitySetupUsageCreateV2DTO entityReferenceDTO =
          EntitySetupUsageCreateV2DTO.newBuilder()
              .setAccountIdentifier(pipelineEntity.getAccountId())
              .setReferredByEntity(pipelineDetails)
              .addAllReferredEntityWithSetupUsageDetail(entityDetailWithSetupUsageDetailProtoDTOS)
              .setDeleteOldReferredByRecords(true)
              .build();
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                  EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, entry.getKey(),
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
    }
  }

  private List<EntityDetailWithSetupUsageDetailProtoDTO> convertToReferredEntityWithSetupUsageDetail(
      List<EntityDetailProtoDTO> entityDetailProtoDTOs, String setupUsageDetailType) {
    List<EntityDetailWithSetupUsageDetailProtoDTO> res = new ArrayList<>();
    for (EntityDetailProtoDTO entityDetailProtoDTO : entityDetailProtoDTOs) {
      String fqn = entityDetailProtoDTO.getIdentifierRef().getMetadataMap().get(PreFlightCheckMetadata.FQN);
      EntityReferredByPipelineDetailProtoDTO entityReferredByPipelineDetailProtoDTO = getSetupDetailProtoDTO(fqn);
      res.add(EntityDetailWithSetupUsageDetailProtoDTO.newBuilder()
                  .setReferredEntity(entityDetailProtoDTO)
                  .setType(setupUsageDetailType)
                  .setEntityInPipelineDetail(entityReferredByPipelineDetailProtoDTO)
                  .build());
    }
    return res;
  }

  private EntityReferredByPipelineDetailProtoDTO getSetupDetailProtoDTO(String fqn) {
    String stageIdentifier = YamlUtils.getStageIdentifierFromFqn(fqn);
    if (stageIdentifier != null) {
      return EntityReferredByPipelineDetailProtoDTO.newBuilder()
          .setIdentifier(stageIdentifier)
          .setType(PipelineDetailType.STAGE_IDENTIFIER)
          .build();
    } else {
      String variableName = Objects.requireNonNull(YamlUtils.getPipelineVariableNameFromFqn(fqn));
      return EntityReferredByPipelineDetailProtoDTO.newBuilder()
          .setIdentifier(variableName)
          .setType(PipelineDetailType.VARIABLE_NAME)
          .build();
    }
  }

  private void deleteSetupUsagesForGivenPipeline(PipelineEntity pipelineEntity) {
    EntityDetailProtoDTO pipelineDetails =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(pipelineEntity.getAccountId(),
                pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
                pipelineEntity.getIdentifier()))
            .setType(EntityTypeProtoEnum.PIPELINES)
            .setName(pipelineEntity.getName())
            .build();

    EntitySetupUsageCreateV2DTO entityReferenceDTO = EntitySetupUsageCreateV2DTO.newBuilder()
                                                         .setAccountIdentifier(pipelineEntity.getAccountId())
                                                         .setReferredByEntity(pipelineDetails)
                                                         .addAllReferredEntities(new ArrayList<>())
                                                         .setDeleteOldReferredByRecords(true)
                                                         .build();
    // Send Events for all refferredEntitiesType so as to delete them
    for (EntityTypeProtoEnum protoEnum : EntityTypeProtoEnum.values()) {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", pipelineEntity.getAccountId(),
                  EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, protoEnum.name(),
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
              .setData(entityReferenceDTO.toByteString())
              .build());
    }
  }

  @Override
  public void onDelete(PipelineEntity pipelineEntity) {
    try {
      deleteSetupUsagesForGivenPipeline(pipelineEntity);
    } catch (EventsFrameworkDownException ex) {
      log.error("Redis Producer shutdown", ex);
    }
  }
}
