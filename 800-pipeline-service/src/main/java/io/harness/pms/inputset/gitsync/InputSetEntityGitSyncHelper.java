package io.harness.pms.inputset.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.InputSetReferenceProtoDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OwnedBy(PIPELINE)
@Singleton
public class InputSetEntityGitSyncHelper extends AbstractGitSdkEntityHandler<InputSetEntity, InputSetYamlDTO>
    implements GitSdkEntityHandlerInterface<InputSetEntity, InputSetYamlDTO> {
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

  @Override
  public InputSetYamlDTO save(String accountIdentifier, String yaml) {
    InputSetEntity initEntity = PMSInputSetElementMapper.toInputSetEntity(accountIdentifier, yaml);
    InputSetEntity savedEntity = pmsInputSetService.create(initEntity);
    return InputSetYamlDTOMapper.toDTO(savedEntity);
  }

  @Override
  public InputSetYamlDTO update(String accountIdentifier, String yaml, ChangeType changeType) {
    InputSetEntity inputSetEntity = PMSInputSetElementMapper.toInputSetEntity(accountIdentifier, yaml);
    return InputSetYamlDTOMapper.toDTO(pmsInputSetService.update(inputSetEntity, changeType));
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

  @Override
  public List<FileChange> listAllEntities(ScopeDetails scopeDetails) {
    return null;
  }

  @Override
  public String getLastObjectIdIfExists(String accountIdentifier, String yaml) {
    final InputSetYamlDTO inputSetYamlDTO = getYamlDTO(yaml);
    final Optional<InputSetEntity> inputSetEntity;
    if (inputSetYamlDTO.getInputSetInfo() != null) {
      final InputSetYamlInfoDTO inputSetInfo = inputSetYamlDTO.getInputSetInfo();
      inputSetEntity = pmsInputSetService.get(accountIdentifier, inputSetInfo.getOrgIdentifier(),
          inputSetInfo.getProjectIdentifier(), inputSetInfo.getPipelineInfoConfig().getIdentifier(),
          inputSetInfo.getIdentifier(), false);
    } else {
      final OverlayInputSetYamlInfoDTO overlayInputSetInfo = inputSetYamlDTO.getOverlayInputSetInfo();
      inputSetEntity = pmsInputSetService.get(accountIdentifier, overlayInputSetInfo.getOrgIdentifier(),
          overlayInputSetInfo.getProjectIdentifier(), overlayInputSetInfo.getPipelineIdentifier(),
          overlayInputSetInfo.getIdentifier(), false);
    }
    return inputSetEntity.map(InputSetEntity::getObjectIdOfYaml).orElse(null);
  }

  @Override
  public InputSetYamlDTO getYamlDTO(String yaml) {
    return InputSetYamlDTOMapper.toDTO(yaml);
  }

  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    final InputSetReferenceProtoDTO inputSetRef = entityReference.getInputSetRef();
    final Optional<InputSetEntity> inputSetEntity =
        pmsInputSetService.get(StringValueUtils.getStringFromStringValue(inputSetRef.getAccountIdentifier()),
            StringValueUtils.getStringFromStringValue(inputSetRef.getOrgIdentifier()),
            StringValueUtils.getStringFromStringValue(inputSetRef.getProjectIdentifier()),
            StringValueUtils.getStringFromStringValue(inputSetRef.getPipelineIdentifier()),
            StringValueUtils.getStringFromStringValue(inputSetRef.getIdentifier()), false);
    return inputSetEntity.get().getYaml();
  }
}
