package io.harness.pms.pipeline.gitsync;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.api.ProducerShutdownException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PipelineYamlDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Supplier;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineEntityGitSyncHelper implements GitSdkEntityHandlerInterface<PipelineEntity, PipelineConfig> {
  private final PMSPipelineService pmsPipelineService;

  @Inject
  public PipelineEntityGitSyncHelper(PMSPipelineService pmsPipelineService) {
    this.pmsPipelineService = pmsPipelineService;
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
  public PipelineConfig save(PipelineConfig dto, String accountIdentifier) {
    PipelineEntity entity = PipelineYamlDtoMapper.toEntity(dto, accountIdentifier);
    return PipelineYamlDtoMapper.toDto(entity);
  }

  @Override
  public PipelineConfig update(PipelineConfig dto, String accountIdentifier) {
    return save(dto, accountIdentifier);
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    try {
      return pmsPipelineService.delete(entityReference.getAccountIdentifier(), entityReference.getOrgIdentifier(),
          entityReference.getProjectIdentifier(), entityReference.getIdentifier(), null);
    } catch (ProducerShutdownException ex) {
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
}
