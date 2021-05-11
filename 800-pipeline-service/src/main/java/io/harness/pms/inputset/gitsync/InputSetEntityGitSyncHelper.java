package io.harness.pms.inputset.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Supplier;

@OwnedBy(PIPELINE)
@Singleton
public class InputSetEntityGitSyncHelper implements GitSdkEntityHandlerInterface<InputSetEntity, InputSetYamlDTO> {
  private final PMSInputSetService pmsInputSetService;

  @Inject
  public InputSetEntityGitSyncHelper(PMSInputSetService pmsInputSetService) {
    this.pmsInputSetService = pmsInputSetService;
  }

  @Override
  public Supplier<InputSetYamlDTO> getYamlFromEntity(InputSetEntity entity) {
    return () -> InputSetYamlDTOMapper.toDTO(entity);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.INPUT_SETS;
  }

  @Override
  public Supplier<InputSetEntity> getEntityFromYaml(InputSetYamlDTO yaml, String accountIdentifier) {
    return () -> InputSetYamlDTOMapper.toEntity(yaml, accountIdentifier);
  }

  @Override
  public EntityDetail getEntityDetail(InputSetEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.INPUT_SETS)
        .entityRef(InputSetReference.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .pipelineIdentifier(entity.getPipelineIdentifier())
                       .identifier(entity.getIdentifier())
                       .build())
        .build();
  }

  // confirm this impl from Garvit
  @Override
  public InputSetYamlDTO save(InputSetYamlDTO yaml, String accountIdentifier) {
    InputSetEntity entity = InputSetYamlDTOMapper.toEntity(yaml, accountIdentifier);
    return InputSetYamlDTOMapper.toDTO(entity);
  }

  @Override
  public InputSetYamlDTO update(InputSetYamlDTO yaml, String accountIdentifier) {
    return save(yaml, accountIdentifier);
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return pmsInputSetService.delete(entityReference.getAccountIdentifier(), entityReference.getOrgIdentifier(),
        entityReference.getProjectIdentifier(), ((InputSetReference) entityReference).getPipelineIdentifier(),
        entityReference.getIdentifier(), null);
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return InputSetEntityKeys.objectIdOfYaml;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return InputSetEntityKeys.isFromDefaultBranch;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return InputSetEntityKeys.yamlGitConfigRef;
  }

  @Override
  public String getUuidKey() {
    return InputSetEntityKeys.uuid;
  }

  @Override
  public String getBranchKey() {
    return InputSetEntityKeys.branch;
  }
}
