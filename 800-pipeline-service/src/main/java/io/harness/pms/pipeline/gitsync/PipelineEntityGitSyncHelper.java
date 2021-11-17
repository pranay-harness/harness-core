package io.harness.pms.pipeline.gitsync;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.mappers.PipelineYamlDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PipelineEntityGitSyncHelper extends AbstractGitSdkEntityHandler<PipelineEntity, PipelineConfig>
    implements GitSdkEntityHandlerInterface<PipelineEntity, PipelineConfig> {
  private final PMSPipelineService pmsPipelineService;
  private final PMSPipelineTemplateHelper pipelineTemplateHelper;
  private final PMSYamlSchemaService pmsYamlSchemaService;

  @Inject
  public PipelineEntityGitSyncHelper(PMSPipelineService pmsPipelineService,
      PMSPipelineTemplateHelper pipelineTemplateHelper, PMSYamlSchemaService pmsYamlSchemaService) {
    this.pmsPipelineService = pmsPipelineService;
    this.pipelineTemplateHelper = pipelineTemplateHelper;
    this.pmsYamlSchemaService = pmsYamlSchemaService;
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.PIPELINES;
  }

  @Override
  public Supplier<PipelineConfig> getYamlFromEntity(PipelineEntity entity) {
    return () -> PipelineYamlDtoMapper.toDto(entity);
  }

  @Override
  public Supplier<PipelineEntity> getEntityFromYaml(PipelineConfig yaml, String accountIdentifier) {
    return () -> PipelineYamlDtoMapper.toEntity(yaml, accountIdentifier);
  }

  @Override
  public EntityDetail getEntityDetail(PipelineEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.PIPELINES)
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .scope(ScopeHelper.getScope(
                           entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier()))
                       .identifier(entity.getIdentifier())
                       .build())
        .build();
  }

  @Override
  public PipelineConfig save(String accountIdentifier, String yaml) {
    PipelineEntity entity = PMSPipelineDtoMapper.toPipelineEntity(accountIdentifier, yaml);
    validate(accountIdentifier, entity);
    PipelineEntity pipelineEntity = pmsPipelineService.create(entity);
    return PipelineYamlDtoMapper.toDto(pipelineEntity);
  }

  @Override
  public PipelineConfig update(String accountIdentifier, String yaml, ChangeType changeType) {
    PipelineEntity entity = PMSPipelineDtoMapper.toPipelineEntity(accountIdentifier, yaml);
    validate(accountIdentifier, entity);
    PipelineEntity pipelineEntity = pmsPipelineService.updatePipelineYaml(entity, changeType);
    return PipelineYamlDtoMapper.toDto(pipelineEntity);
  }

  private void validate(String accountIdentifier, PipelineEntity entity) {
    TemplateMergeResponseDTO templateMergeResponseDTO = pipelineTemplateHelper.resolveTemplateRefsInPipeline(entity);
    if (EmptyPredicate.isNotEmpty(templateMergeResponseDTO.getTemplateReferenceSummaries())) {
      entity.setTemplateReference(true);
    }
    pmsYamlSchemaService.validateYamlSchema(accountIdentifier, entity.getOrgIdentifier(), entity.getProjectIdentifier(),
        templateMergeResponseDTO.getMergedPipelineYaml());
    // validate unique fqn in resolveTemplateRefsInPipeline
    try {
      pmsYamlSchemaService.validateUniqueFqn(templateMergeResponseDTO.getMergedPipelineYaml());
    } catch (IOException e) {
      log.error("Error when trying to validate for Unique FQNs", e);
      throw new InvalidRequestException("Error when trying to validate for Unique FQNs", e);
    }
  }

  @Override
  public boolean markEntityInvalid(String accountIdentifier, EntityReference entityReference, String erroneousYaml) {
    return false;
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    try {
      return pmsPipelineService.delete(entityReference.getAccountIdentifier(), entityReference.getOrgIdentifier(),
          entityReference.getProjectIdentifier(), entityReference.getIdentifier(), null);
    } catch (EventsFrameworkDownException ex) {
      throw new UnexpectedException("Producer shutdown: " + ExceptionUtils.getMessage(ex));
    }
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return PipelineEntityKeys.objectIdOfYaml;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return PipelineEntityKeys.isFromDefaultBranch;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return PipelineEntityKeys.yamlGitConfigRef;
  }

  @Override
  public String getUuidKey() {
    return PipelineEntityKeys.uuid;
  }

  @Override
  public String getBranchKey() {
    return PipelineEntityKeys.branch;
  }

  @Override
  public List<FileChange> listAllEntities(ScopeDetails scopeDetails) {
    return null;
  }

  @Override
  public String getLastObjectIdIfExists(String accountIdentifier, String yaml) {
    final PipelineConfig pipelineConfig = getYamlDTO(yaml);
    final PipelineInfoConfig pipelineInfoConfig = pipelineConfig.getPipelineInfoConfig();
    final Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountIdentifier, pipelineInfoConfig.getOrgIdentifier(),
            pipelineInfoConfig.getProjectIdentifier(), pipelineInfoConfig.getIdentifier(), false);
    return pipelineEntity.map(PipelineEntity::getObjectIdOfYaml).orElse(null);
  }

  @Override
  public PipelineConfig getYamlDTO(String yaml) {
    return PipelineYamlDtoMapper.toDto(yaml);
  }

  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    final IdentifierRefProtoDTO identifierRef = entityReference.getIdentifierRef();
    final Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(StringValueUtils.getStringFromStringValue(identifierRef.getAccountIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getOrgIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getProjectIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getIdentifier()), false);
    return pipelineEntity.get().getYaml();
  }
}
