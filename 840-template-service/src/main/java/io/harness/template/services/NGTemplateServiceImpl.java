package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.repositories.NGTemplateRepository;
import io.harness.springdata.TransactionHelper;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.gitsync.TemplateGitSyncBranchContextGuard;
import io.harness.template.mappers.NGTemplateDtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(CDC)
public class NGTemplateServiceImpl implements NGTemplateService {
  @Inject private NGTemplateRepository templateRepository;
  @Inject private NGTemplateServiceHelper templateServiceHelper;
  @Inject private GitSyncSdkService gitSyncSdkService;
  @Inject private TransactionHelper transactionHelper;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Template [%s] of label [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public TemplateEntity create(TemplateEntity templateEntity) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());

      // Check if this is template identifier first entry, for marking it as stable template.
      if (checkIfGivenTemplateShouldBeMarkedStable(templateEntity.getAccountId(), templateEntity.getOrgIdentifier(),
              templateEntity.getProjectIdentifier(), templateEntity.getIdentifier(), false)) {
        templateEntity = templateEntity.withStableTemplate(true);
      }

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
        // sending old entity as null here because a new mongo entity will be created. If audit trail needs to be added
        // to git synced projects, a get call needs to be added here to the base branch of this template update
        return makeTemplateUpdateCall(templateEntity, null, changeType);
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
      TemplateEntity oldTemplateEntity = optionalTemplate.get();
      TemplateEntity templateToUpdate = oldTemplateEntity.withYaml(templateEntity.getYaml())
                                            .withTemplateScope(templateEntity.getTemplateScope())
                                            .withName(templateEntity.getName())
                                            .withDescription(templateEntity.getDescription())
                                            .withTags(templateEntity.getTags())
                                            .withOrgIdentifier(templateEntity.getOrgIdentifier())
                                            .withProjectIdentifier(templateEntity.getProjectIdentifier())
                                            .withTemplateEntityType(templateEntity.getTemplateEntityType())
                                            .withChildType(templateEntity.getChildType())
                                            .withFullyQualifiedIdentifier(templateEntity.getFullyQualifiedIdentifier());
      return makeTemplateUpdateCall(templateToUpdate, oldTemplateEntity, changeType);
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
      if (EmptyPredicate.isEmpty(versionLabel)) {
        return templateRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsStableAndDeletedNot(
                accountId, orgIdentifier, projectIdentifier, templateIdentifier, !deleted);
      }
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

  @Override
  public boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier,
      String versionLabel, Long version) {
    Optional<TemplateEntity> optionalTemplateEntity =
        get(accountId, orgIdentifier, projectIdentifier, templateIdentifier, versionLabel, false);
    if (!optionalTemplateEntity.isPresent()) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] does not exist.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
    }

    TemplateEntity existingTemplate = optionalTemplateEntity.get();
    if (version != null && !version.equals(existingTemplate.getVersion())) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] is not on the correct version.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
    }
    if (existingTemplate.isStableTemplate()) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] is a stable template, thus cannot delete it.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
    }
    TemplateEntity withDeleted = existingTemplate.withDeleted(true);
    try {
      TemplateEntity deletedTemplate =
          templateRepository.deleteTemplate(withDeleted, NGTemplateDtoMapper.toDTO(withDeleted));
      if (deletedTemplate.getDeleted()) {
        return true;
      } else {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] couldn't be deleted.",
            templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting template with identifier [%s] and versionLabel [%s]",
                    templateIdentifier, versionLabel),
          e);
      throw new InvalidRequestException(
          String.format("Error while deleting template with identifier [%s] and versionLabel [%s]: %s",
              templateIdentifier, versionLabel, e.getMessage()));
    }
  }

  @Override
  public Page<TemplateEntity> list(Criteria criteria, Pageable pageable, String accountId, String orgIdentifier,
      String projectIdentifier, Boolean getDistinctFromBranches) {
    if (Boolean.TRUE.equals(getDistinctFromBranches)
        && gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return templateRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, true);
    }
    return templateRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, false);
  }

  @Override
  public TemplateEntity updateStableTemplateVersion(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String templateIdentifier, String versionLabel,
      GitEntityFindInfoDTO gitEntityBasicInfo) {
    return transactionHelper.performTransaction(
        ()
            -> updateStableTemplateVersionHelper(accountIdentifier, orgIdentifier, projectIdentifier,
                templateIdentifier, versionLabel, gitEntityBasicInfo));
  }

  private TemplateEntity updateStableTemplateVersionHelper(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String templateIdentifier, String versionLabel,
      GitEntityFindInfoDTO gitEntityBasicInfo) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(accountIdentifier, templateIdentifier, versionLabel);
      Optional<TemplateEntity> optionalTemplateEntity =
          templateRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsStableAndDeletedNot(
              accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, true);
      if (optionalTemplateEntity.isPresent()) {
        // make previous stable template as false.
        TemplateEntity oldTemplate = optionalTemplateEntity.get();

        try (TemplateGitSyncBranchContextGuard ignored =
                 templateServiceHelper.getTemplateGitContext(oldTemplate, gitEntityBasicInfo,
                     format("Template with identifier [%s] and versionLabel [%s] marking stable template as false.",
                         templateIdentifier, oldTemplate.getVersionLabel()))) {
          TemplateEntity templateToUpdate = oldTemplate.withStableTemplate(false);
          makeTemplateUpdateCall(templateToUpdate, oldTemplate, ChangeType.MODIFY);
        }
      }
      optionalTemplateEntity =
          get(accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, versionLabel, false);
      if (!optionalTemplateEntity.isPresent()) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] does not exist.",
            templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
      }
      // make current version stable template as true.
      TemplateEntity oldTemplateForGivenVersion = optionalTemplateEntity.get();
      try (TemplateGitSyncBranchContextGuard ignored =
               templateServiceHelper.getTemplateGitContext(oldTemplateForGivenVersion, gitEntityBasicInfo,
                   format("Template with identifier [%s] and versionLabel [%s] marking stable template as true.",
                       templateIdentifier, versionLabel))) {
        TemplateEntity templateToUpdateForGivenVersion = oldTemplateForGivenVersion.withStableTemplate(true);
        return makeTemplateUpdateCall(templateToUpdateForGivenVersion, oldTemplateForGivenVersion, ChangeType.MODIFY);
      }
    } catch (Exception e) {
      log.error(
          String.format("Error while updating template with identifier [%s] to stable template of versionLabel [%s]",
              templateIdentifier, versionLabel),
          e);
      throw new InvalidRequestException(String.format(
          "Error while updating template with identifier [%s] to stable template of versionLabel [%s]: %s",
          templateIdentifier, versionLabel, ExceptionUtils.getMessage(e)));
    }
  }

  private TemplateEntity makeTemplateUpdateCall(
      TemplateEntity templateToUpdate, TemplateEntity oldTemplateEntity, ChangeType changeType) {
    try {
      TemplateEntity updatedTemplate = templateRepository.updateTemplateYaml(
          templateToUpdate, oldTemplateEntity, NGTemplateDtoMapper.toDTO(templateToUpdate), changeType);
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

  private boolean checkIfGivenTemplateShouldBeMarkedStable(
      String accountId, String orgIdentifier, String projectIdentifier, String templateIdentifier, boolean markStable) {
    if (markStable) {
      return true;
    }
    Criteria criteria = templateServiceHelper.formCriteria(accountId, orgIdentifier, projectIdentifier, "",
        TemplateFilterPropertiesDTO.builder()
            .templateIdentifiers(Collections.singletonList(templateIdentifier))
            .build(),
        false, "");
    PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, TemplateEntityKeys.lastUpdatedAt));
    Page<TemplateEntity> templateEntities =
        list(criteria, pageRequest, accountId, orgIdentifier, projectIdentifier, false);
    return templateEntities.getContent().isEmpty();
  }
}
