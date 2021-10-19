package io.harness.template.gitsync;

import static io.harness.ng.core.utils.NGUtils.validate;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.NGTemplateReference;
import io.harness.common.EntityReference;
import io.harness.encryption.ScopeHelper;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.ng.core.EntityDetail;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.beans.yaml.NGTemplateInfoConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.mappers.NGTemplateDtoMapper;
import io.harness.template.services.NGTemplateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class TemplateEntityGitSyncHandler extends AbstractGitSdkEntityHandler<TemplateEntity, NGTemplateConfig>
    implements GitSdkEntityHandlerInterface<TemplateEntity, NGTemplateConfig> {
  private final NGTemplateService templateService;

  @Inject
  public TemplateEntityGitSyncHandler(NGTemplateService templateService) {
    this.templateService = templateService;
  }

  @Override
  public NGTemplateConfig getYamlDTO(String yaml) {
    return NGTemplateDtoMapper.toDTO(yaml);
  }

  @Override
  public Supplier<NGTemplateConfig> getYamlFromEntity(TemplateEntity entity) {
    return () -> NGTemplateDtoMapper.toDTO(entity);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.TEMPLATE;
  }

  @Override
  public Supplier<TemplateEntity> getEntityFromYaml(NGTemplateConfig templateConfig, String accountIdentifier) {
    return () -> NGTemplateDtoMapper.toTemplateEntity(accountIdentifier, templateConfig);
  }

  @Override
  public EntityDetail getEntityDetail(TemplateEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.TEMPLATE)
        .entityRef(NGTemplateReference.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .scope(ScopeHelper.getScope(
                           entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier()))
                       .identifier(entity.getIdentifier())
                       .versionLabel(entity.getVersionLabel())
                       .build())
        .build();
  }

  @Override
  public NGTemplateConfig save(String accountIdentifier, String yaml) {
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(accountIdentifier, yaml);
    validate(templateEntity);
    TemplateEntity createdTemplate = templateService.create(templateEntity, false, "");
    return NGTemplateDtoMapper.toDTO(createdTemplate);
  }

  @Override
  public NGTemplateConfig update(String accountIdentifier, String yaml, ChangeType changeType) {
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(accountIdentifier, yaml);
    validate(templateEntity);
    return NGTemplateDtoMapper.toDTO(templateService.updateTemplateEntity(templateEntity, changeType, false, ""));
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    try {
      NGTemplateReference reference = (NGTemplateReference) entityReference;
      return templateService.delete(entityReference.getAccountIdentifier(), entityReference.getOrgIdentifier(),
          entityReference.getProjectIdentifier(), entityReference.getIdentifier(), reference.getVersionLabel(), null,
          "");
    } catch (EventsFrameworkDownException ex) {
      throw new UnexpectedException("Producer shutdown: " + ExceptionUtils.getMessage(ex));
    }
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return TemplateEntityKeys.objectIdOfYaml;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return TemplateEntityKeys.isFromDefaultBranch;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return TemplateEntityKeys.yamlGitConfigRef;
  }

  @Override
  public String getUuidKey() {
    return TemplateEntityKeys.uuid;
  }

  @Override
  public String getBranchKey() {
    return TemplateEntityKeys.branch;
  }

  // todo(archit): implement
  @Override
  public List<FileChange> listAllEntities(ScopeDetails scopeDetails) {
    return null;
  }

  @Override
  public String getLastObjectIdIfExists(String accountIdentifier, String yaml) {
    NGTemplateConfig yamlDTO = getYamlDTO(yaml);
    NGTemplateInfoConfig templateInfoConfig = yamlDTO.getTemplateInfoConfig();
    Optional<TemplateEntity> templateEntity = templateService.get(accountIdentifier,
        templateInfoConfig.getOrgIdentifier(), templateInfoConfig.getProjectIdentifier(),
        templateInfoConfig.getIdentifier(), templateInfoConfig.getVersionLabel(), false);
    return templateEntity.map(TemplateEntity::getObjectIdOfYaml).orElse(null);
  }

  // todo(archit): implement
  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    return null;
  }
}
