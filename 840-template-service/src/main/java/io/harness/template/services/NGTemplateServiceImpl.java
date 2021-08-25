package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.repositories.NGTemplateRepository;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.mappers.NGTemplateDtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
@OwnedBy(CDC)
public class NGTemplateServiceImpl implements NGTemplateService {
  @Inject private NGTemplateRepository templateRepository;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Template [%s] of label [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public TemplateEntity create(TemplateEntity templateEntity) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());
      return templateRepository.save(templateEntity, NGTemplateDtoMapper.toDTO(templateEntity));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, templateEntity.getIdentifier(), templateEntity.getVersionLabel(),
              templateEntity.getProjectIdentifier(), templateEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      log.error(String.format("Error while saving template [%s] of label [%s]", templateEntity.getIdentifier(),
                    templateEntity.getVersionLabel()),
          e);
      throw new InvalidRequestException(String.format("Error while saving template [%s] of label [%s]: %s",
          templateEntity.getIdentifier(), templateEntity.getVersionLabel(), e.getMessage()));
    }
  }

  @Override
  public TemplateEntity updateTemplateEntity(TemplateEntity templateEntity, ChangeType changeType) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());

      if (GitContextHelper.getGitEntityInfo() != null && GitContextHelper.getGitEntityInfo().isNewBranch()) {
        return makeTemplateUpdateCall(templateEntity, changeType);
      }

      Optional<TemplateEntity> optionalTemplate =
          templateRepository
              .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
                  templateEntity.getAccountId(), templateEntity.getOrgIdentifier(),
                  templateEntity.getProjectIdentifier(), templateEntity.getIdentifier(),
                  templateEntity.getVersionLabel(), true);

      if (!optionalTemplate.isPresent()) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] doesn't exist.",
            templateEntity.getIdentifier(), templateEntity.getVersionLabel(), templateEntity.getProjectIdentifier(),
            templateEntity.getOrgIdentifier()));
      }
      TemplateEntity templateToUpdate = optionalTemplate.get();
      templateToUpdate = templateToUpdate.withYaml(templateEntity.getYaml())
                             .withTemplateScope(templateEntity.getTemplateScope())
                             .withName(templateEntity.getName())
                             .withDescription(templateEntity.getDescription())
                             .withTags(templateEntity.getTags())
                             .withOrgIdentifier(templateEntity.getOrgIdentifier())
                             .withProjectIdentifier(templateEntity.getProjectIdentifier())
                             .withTemplateEntityType(templateEntity.getTemplateEntityType())
                             .withChildType(templateEntity.getChildType())
                             .withFullyQualifiedIdentifier(templateEntity.getFullyQualifiedIdentifier());
      return makeTemplateUpdateCall(templateToUpdate, changeType);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, templateEntity.getIdentifier(), templateEntity.getVersionLabel(),
              templateEntity.getProjectIdentifier(), templateEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      log.error(String.format("Error while saving template [%s] of versionLabel [%s]", templateEntity.getIdentifier(),
                    templateEntity.getVersionLabel()),
          e);
      throw new InvalidRequestException(String.format("Error while saving template [%s] of label [%s]: %s",
          templateEntity.getIdentifier(), templateEntity.getVersionLabel(), e.getMessage()));
    }
  }

  @Override
  public Optional<TemplateEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, String versionLabel, boolean deleted) {
    try {
      return templateRepository
          .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
              accountId, orgIdentifier, projectIdentifier, templateIdentifier, versionLabel, !deleted);
    } catch (Exception e) {
      log.error(String.format("Error while retrieving template with identifier [%s] and versionLabel [%s]",
                    templateIdentifier, versionLabel),
          e);
      throw new InvalidRequestException(
          String.format("Error while retrieving template with identifier [%s] and versionLabel [%s]: %s",
              templateIdentifier, versionLabel, ExceptionUtils.getMessage(e)));
    }
  }

  private TemplateEntity makeTemplateUpdateCall(TemplateEntity templateToUpdate, ChangeType changeType) {
    try {
      TemplateEntity updatedTemplate = templateRepository.updateTemplateYaml(
          templateToUpdate, NGTemplateDtoMapper.toDTO(templateToUpdate), changeType);
      if (updatedTemplate == null) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] could not be updated.",
            templateToUpdate.getIdentifier(), templateToUpdate.getVersionLabel(),
            templateToUpdate.getProjectIdentifier(), templateToUpdate.getOrgIdentifier()));
      }
      return updatedTemplate;
    } catch (Exception e) {
      log.error(String.format("Error while updating template with identifier [%s] and versionLabel [%s]",
                    templateToUpdate.getIdentifier(), templateToUpdate.getVersionLabel()),
          e);
      throw new InvalidRequestException(
          String.format("Error while updating template with identifier [%s] and versionLabel [%s] : %s",
              templateToUpdate.getIdentifier(), templateToUpdate.getVersionLabel(), e.getMessage()));
    }
  }
}
