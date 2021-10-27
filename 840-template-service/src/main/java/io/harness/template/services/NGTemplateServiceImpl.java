package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.Scope;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.repositories.NGTemplateRepository;
import io.harness.springdata.TransactionHelper;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.events.TemplateUpdateEventType;
import io.harness.template.gitsync.TemplateGitSyncBranchContextGuard;
import io.harness.template.mappers.NGTemplateDtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  public TemplateEntity create(TemplateEntity templateEntity, boolean setDefaultTemplate, String comments) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());

      // Check if this is template identifier first entry, for marking it as stable template.
      boolean firstVersionEntry =
          getAllTemplatesForGivenIdentifier(templateEntity.getAccountId(), templateEntity.getOrgIdentifier(),
              templateEntity.getProjectIdentifier(), templateEntity.getIdentifier(), false)
              .isEmpty();
      if (firstVersionEntry || setDefaultTemplate) {
        templateEntity = templateEntity.withStableTemplate(true);
      }

      // a new template creation always means this is now the lastUpdated template.
      templateEntity = templateEntity.withLastUpdatedTemplate(true);

      // check to make previous template stable as false
      TemplateEntity finalTemplateEntity = templateEntity;
      if (!firstVersionEntry && setDefaultTemplate) {
        return transactionHelper.performTransaction(() -> {
          makePreviousStableTemplateFalse(finalTemplateEntity.getAccountIdentifier(),
              finalTemplateEntity.getOrgIdentifier(), finalTemplateEntity.getProjectIdentifier(),
              finalTemplateEntity.getIdentifier(), finalTemplateEntity.getVersionLabel());
          makePreviousLastUpdatedTemplateFalse(finalTemplateEntity.getAccountIdentifier(),
              finalTemplateEntity.getOrgIdentifier(), finalTemplateEntity.getProjectIdentifier(),
              finalTemplateEntity.getIdentifier());
          return templateRepository.save(finalTemplateEntity, NGTemplateDtoMapper.toDTO(finalTemplateEntity), comments);
        });
      } else {
        return transactionHelper.performTransaction(() -> {
          makePreviousLastUpdatedTemplateFalse(finalTemplateEntity.getAccountIdentifier(),
              finalTemplateEntity.getOrgIdentifier(), finalTemplateEntity.getProjectIdentifier(),
              finalTemplateEntity.getIdentifier());
          return templateRepository.save(finalTemplateEntity, NGTemplateDtoMapper.toDTO(finalTemplateEntity), comments);
        });
      }

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
  public TemplateEntity updateTemplateEntity(
      TemplateEntity templateEntity, ChangeType changeType, boolean setDefaultTemplate, String comments) {
    return transactionHelper.performTransaction(() -> {
      makePreviousLastUpdatedTemplateFalse(templateEntity.getAccountIdentifier(), templateEntity.getOrgIdentifier(),
          templateEntity.getProjectIdentifier(), templateEntity.getIdentifier());
      return updateTemplateHelper(templateEntity.getOrgIdentifier(), templateEntity.getProjectIdentifier(),
          templateEntity, changeType, setDefaultTemplate, true, comments);
    });
  }

  private TemplateEntity updateTemplateHelper(String oldOrgIdentifier, String oldProjectIdentifier,
      TemplateEntity templateEntity, ChangeType changeType, boolean setDefaultTemplate,
      boolean updateLastUpdatedTemplateFlag, String comments) {
    try {
      NGTemplateServiceHelper.validatePresenceOfRequiredFields(
          templateEntity.getAccountId(), templateEntity.getIdentifier(), templateEntity.getVersionLabel());

      GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
      if (gitEntityInfo != null && gitEntityInfo.isNewBranch()) {
        // sending old entity as null here because a new mongo entity will be created. If audit trail needs to be added
        // to git synced projects, a get call needs to be added here to the base branch of this template update
        return makeTemplateUpdateCall(templateEntity, null, changeType, comments, TemplateUpdateEventType.OTHERS_EVENT);
      }

      Optional<TemplateEntity> optionalTemplate =
          templateRepository
              .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndVersionLabelAndDeletedNot(
                  templateEntity.getAccountId(), oldOrgIdentifier, oldProjectIdentifier, templateEntity.getIdentifier(),
                  templateEntity.getVersionLabel(), true);

      if (!optionalTemplate.isPresent()) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] doesn't exist.",
            templateEntity.getIdentifier(), templateEntity.getVersionLabel(), templateEntity.getProjectIdentifier(),
            templateEntity.getOrgIdentifier()));
      }
      TemplateEntity oldTemplateEntity = optionalTemplate.get();
      if (!oldTemplateEntity.getTemplateEntityType().equals(templateEntity.getTemplateEntityType())) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] cannot update the template type, type is [%s].",
            templateEntity.getIdentifier(), templateEntity.getVersionLabel(), templateEntity.getProjectIdentifier(),
            templateEntity.getOrgIdentifier(), oldTemplateEntity.getTemplateEntityType()));
      }
      if (!oldTemplateEntity.getChildType().equals(templateEntity.getChildType())) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] cannot update the internal template type, type is [%s].",
            templateEntity.getIdentifier(), templateEntity.getVersionLabel(), templateEntity.getProjectIdentifier(),
            templateEntity.getOrgIdentifier(), oldTemplateEntity.getChildType()));
      }

      TemplateEntity templateToUpdate = oldTemplateEntity.withYaml(templateEntity.getYaml())
                                            .withTemplateScope(templateEntity.getTemplateScope())
                                            .withName(templateEntity.getName())
                                            .withDescription(templateEntity.getDescription())
                                            .withTags(templateEntity.getTags())
                                            .withOrgIdentifier(templateEntity.getOrgIdentifier())
                                            .withProjectIdentifier(templateEntity.getProjectIdentifier())
                                            .withFullyQualifiedIdentifier(templateEntity.getFullyQualifiedIdentifier())
                                            .withLastUpdatedTemplate(updateLastUpdatedTemplateFlag);

      // Updating the stable template version.
      if (setDefaultTemplate && !templateToUpdate.isStableTemplate()) {
        TemplateEntity templateToUpdateWithStable = templateToUpdate.withStableTemplate(true);
        return transactionHelper.performTransaction(() -> {
          makePreviousStableTemplateFalse(templateEntity.getAccountIdentifier(), templateEntity.getOrgIdentifier(),
              templateEntity.getProjectIdentifier(), templateEntity.getIdentifier(),
              templateToUpdate.getVersionLabel());
          return makeTemplateUpdateCall(templateToUpdateWithStable, oldTemplateEntity, changeType, "",
              TemplateUpdateEventType.TEMPLATE_STABLE_TRUE_WITH_YAML_CHANGE_EVENT);
        });
      }

      return makeTemplateUpdateCall(
          templateToUpdate, oldTemplateEntity, changeType, "", TemplateUpdateEventType.OTHERS_EVENT);
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
      String versionLabel, Long version, String comments) {
    List<TemplateEntity> templateEntities =
        getAllTemplatesForGivenIdentifier(accountId, orgIdentifier, projectIdentifier, templateIdentifier, false);

    return deleteTemplateHelper(accountId, orgIdentifier, projectIdentifier, templateIdentifier, versionLabel, version,
        templateEntities, templateEntities.size() == 1, comments);
  }

  @Override
  public boolean deleteTemplates(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, Set<String> templateVersions, String comments) {
    List<TemplateEntity> templateEntities =
        getAllTemplatesForGivenIdentifier(accountId, orgIdentifier, projectIdentifier, templateIdentifier, false);
    boolean canDeleteStableTemplate = templateEntities.size() == templateVersions.size();
    for (TemplateEntity templateEntity : templateEntities) {
      if (templateVersions.contains(templateEntity.getVersionLabel())) {
        boolean templateDeleted = deleteTemplateHelper(accountId, orgIdentifier, projectIdentifier, templateIdentifier,
            templateEntity.getVersionLabel(), null, templateEntities, canDeleteStableTemplate, comments);
        if (!templateDeleted) {
          throw new InvalidRequestException(
              String.format("Error while deleting multiple templates with identifier: %s", templateIdentifier));
        }
      }
    }
    return true;
  }

  private boolean deleteTemplateHelper(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, String versionLabel, Long version, List<TemplateEntity> allTemplateEntities,
      boolean canDeleteStableTemplate, String comments) {
    // find the given template version in the list
    TemplateEntity existingTemplate =
        allTemplateEntities.stream()
            .filter(templateEntity -> templateEntity.getVersionLabel().equals(versionLabel))
            .findFirst()
            .orElse(null);
    if (existingTemplate == null) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s], Account [%s] does not exist.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier, accountId));
    }

    if (version != null && !version.equals(existingTemplate.getVersion())) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] is not on the correct version.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
    }

    // Check if template is stable whether it can be deleted or not.
    // Can delete stable template only if that's the only template version left.
    if (existingTemplate.isStableTemplate() && !canDeleteStableTemplate) {
      throw new InvalidRequestException(format(
          "Template with identifier [%s] and versionLabel [%s], under Project[%s], Organization [%s] is a stable template, thus cannot delete it.",
          templateIdentifier, versionLabel, projectIdentifier, orgIdentifier));
    }
    TemplateEntity withDeleted = existingTemplate.withDeleted(true);
    try {
      TemplateEntity deletedTemplate =
          templateRepository.deleteTemplate(withDeleted, NGTemplateDtoMapper.toDTO(withDeleted), comments);
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
      String projectIdentifier, String templateIdentifier, String newStableTemplateVersion) {
    return transactionHelper.performTransaction(
        ()
            -> updateStableTemplateVersionHelper(
                accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, newStableTemplateVersion));
  }

  @Override
  public boolean updateTemplateSettings(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, Scope currentScope, Scope updateScope, String updateStableTemplateVersion,
      Boolean getDistinctFromBranches) {
    // if both current and update scope of template are same, check for updating stable template version
    if (currentScope.equals(updateScope)) {
      String orgIdBasedOnScope = currentScope.equals(Scope.ACCOUNT) ? null : orgIdentifier;
      String projectIdBasedOnScope = currentScope.equals(Scope.PROJECT) ? projectIdentifier : null;
      TemplateEntity entity = updateStableTemplateVersion(
          accountId, orgIdBasedOnScope, projectIdBasedOnScope, templateIdentifier, updateStableTemplateVersion);
      return entity.isStableTemplate();
    } else {
      return transactionHelper.performTransaction(
          ()
              -> updateTemplateScope(accountId, orgIdentifier, projectIdentifier, templateIdentifier, currentScope,
                  updateScope, updateStableTemplateVersion, getDistinctFromBranches));
    }
  }

  private TemplateEntity updateStableTemplateVersionHelper(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String templateIdentifier, String newStableTemplateVersion) {
    try {
      makePreviousStableTemplateFalse(
          accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, newStableTemplateVersion);
      makePreviousLastUpdatedTemplateFalse(accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier);
      Optional<TemplateEntity> optionalTemplateEntity =
          get(accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, newStableTemplateVersion, false);
      if (!optionalTemplateEntity.isPresent()) {
        throw new InvalidRequestException(format(
            "Template with identifier [%s] and versionLabel [%s] under Project[%s], Organization [%s] does not exist.",
            templateIdentifier, newStableTemplateVersion, projectIdentifier, orgIdentifier));
      }
      // make given version stable template as true.
      TemplateEntity oldTemplateForGivenVersion = optionalTemplateEntity.get();

      try (TemplateGitSyncBranchContextGuard ignored = templateServiceHelper.getTemplateGitContextForGivenTemplate(
               oldTemplateForGivenVersion, GitContextHelper.getGitEntityInfo(),
               format("Template with identifier [%s] and versionLabel [%s] marking stable template as true.",
                   templateIdentifier, newStableTemplateVersion))) {
        TemplateEntity templateToUpdateForGivenVersion =
            oldTemplateForGivenVersion.withStableTemplate(true).withLastUpdatedTemplate(true);
        return makeTemplateUpdateCall(templateToUpdateForGivenVersion, oldTemplateForGivenVersion, ChangeType.MODIFY,
            "", TemplateUpdateEventType.TEMPLATE_STABLE_TRUE_EVENT);
      }
    } catch (Exception e) {
      log.error(
          String.format("Error while updating template with identifier [%s] to stable template of versionLabel [%s]",
              templateIdentifier, newStableTemplateVersion),
          e);
      throw new InvalidRequestException(String.format(
          "Error while updating template with identifier [%s] to stable template of versionLabel [%s]: %s",
          templateIdentifier, newStableTemplateVersion, ExceptionUtils.getMessage(e)));
    }
  }

  private TemplateEntity makeTemplateUpdateCall(TemplateEntity templateToUpdate, TemplateEntity oldTemplateEntity,
      ChangeType changeType, String comments, TemplateUpdateEventType templateUpdateEventType) {
    try {
      TemplateEntity updatedTemplate = templateRepository.updateTemplateYaml(templateToUpdate, oldTemplateEntity,
          NGTemplateDtoMapper.toDTO(templateToUpdate), changeType, comments, templateUpdateEventType);
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

  // Current scope is template original scope, updatedScope is new scope.
  private boolean updateTemplateScope(String accountId, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, Scope currentScope, Scope updatedScope, String updateStableTemplateVersion,
      Boolean getDistinctFromBranches) {
    String orgIdBasedOnCurrentScope = currentScope.equals(Scope.ACCOUNT) ? null : orgIdentifier;
    String projectIdBasedOnCurrentScope = currentScope.equals(Scope.PROJECT) ? projectIdentifier : null;
    List<TemplateEntity> templateEntities = getAllTemplatesForGivenIdentifier(
        accountId, orgIdBasedOnCurrentScope, projectIdBasedOnCurrentScope, templateIdentifier, getDistinctFromBranches);

    // Iterating templates to update each entry individually.
    String newOrgIdentifier = null;
    String newProjectIdentifier = null;
    for (int i = 0; i < templateEntities.size(); i++) {
      TemplateEntity templateEntity = templateEntities.get(i);
      NGTemplateConfig templateConfig = NGTemplateDtoMapper.toDTO(templateEntity);
      // set appropriate scope as given by customer
      if (updatedScope.equals(Scope.ORG)) {
        newOrgIdentifier = orgIdentifier;
      } else if (updatedScope.equals(Scope.PROJECT)) {
        newProjectIdentifier = projectIdentifier;
        newOrgIdentifier = orgIdentifier;
      }
      templateConfig.getTemplateInfoConfig().setProjectIdentifier(newProjectIdentifier);
      templateConfig.getTemplateInfoConfig().setOrgIdentifier(newOrgIdentifier);

      TemplateEntity updateEntity = NGTemplateDtoMapper.toTemplateEntity(accountId, templateConfig);
      try (TemplateGitSyncBranchContextGuard ignored = templateServiceHelper.getTemplateGitContextForGivenTemplate(
               updateEntity, GitContextHelper.getGitEntityInfo(),
               format("Template with identifier [%s] and versionLabel [%s] updating the template scope to [%s].",
                   templateIdentifier, updateEntity.getVersionLabel(), updateEntity.getTemplateScope()))) {
        String orgIdBasedOnScope = currentScope.equals(Scope.ACCOUNT) ? null : orgIdentifier;
        String projectIdBasedOnScope = currentScope.equals(Scope.PROJECT) ? projectIdentifier : null;

        // Updating the template
        boolean isLastEntity = i == templateEntities.size() - 1;
        updateTemplateHelper(orgIdBasedOnScope, projectIdBasedOnScope, updateEntity, ChangeType.MODIFY, false,
            isLastEntity, "Changing scope from " + currentScope + " to new scope - " + updatedScope);
      }
    }
    TemplateEntity entity = updateStableTemplateVersion(
        accountId, newOrgIdentifier, newProjectIdentifier, templateIdentifier, updateStableTemplateVersion);
    return entity.isStableTemplate();
  }

  private void makePreviousStableTemplateFalse(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String templateIdentifier, String updatedStableTemplateVersion) {
    NGTemplateServiceHelper.validatePresenceOfRequiredFields(accountIdentifier, templateIdentifier);
    Optional<TemplateEntity> optionalTemplateEntity =
        templateRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsStableAndDeletedNot(
            accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, true);
    if (optionalTemplateEntity.isPresent()) {
      // make previous stable template as false.
      TemplateEntity oldTemplate = optionalTemplateEntity.get();
      if (updatedStableTemplateVersion.equals(oldTemplate.getVersionLabel())) {
        log.info(
            "Ignoring marking previous stable template as false, as new versionLabel given is same as already existing one.");
        return;
      }

      // Update the git context with details of the template on which the operation is going to run.
      try (TemplateGitSyncBranchContextGuard ignored = templateServiceHelper.getTemplateGitContextForGivenTemplate(
               oldTemplate, GitContextHelper.getGitEntityInfo(),
               format("Template with identifier [%s] and versionLabel [%s] marking stable template as false.",
                   templateIdentifier, oldTemplate.getVersionLabel()))) {
        TemplateEntity templateToUpdate = oldTemplate.withStableTemplate(false);
        makeTemplateUpdateCall(
            templateToUpdate, oldTemplate, ChangeType.MODIFY, "", TemplateUpdateEventType.TEMPLATE_STABLE_FALSE_EVENT);
      }
    }
  }

  private void makePreviousLastUpdatedTemplateFalse(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String templateIdentifier) {
    NGTemplateServiceHelper.validatePresenceOfRequiredFields(accountIdentifier, templateIdentifier);
    Optional<TemplateEntity> optionalTemplateEntity =
        templateRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndIsLastUpdatedAndDeletedNot(
                accountIdentifier, orgIdentifier, projectIdentifier, templateIdentifier, true);
    if (optionalTemplateEntity.isPresent()) {
      // make previous last updated template as false.
      TemplateEntity oldTemplate = optionalTemplateEntity.get();

      // Update the git context with details of the template on which the operation is going to run.
      try (TemplateGitSyncBranchContextGuard ignored = templateServiceHelper.getTemplateGitContextForGivenTemplate(
               oldTemplate, GitContextHelper.getGitEntityInfo(),
               format("Template with identifier [%s] and versionLabel [%s] marking last updated template as false.",
                   templateIdentifier, oldTemplate.getVersionLabel()))) {
        TemplateEntity templateToUpdate = oldTemplate.withLastUpdatedTemplate(false);
        makeTemplateUpdateCall(templateToUpdate, oldTemplate, ChangeType.MODIFY, "",
            TemplateUpdateEventType.TEMPLATE_LAST_UPDATED_FALSE_EVENT);
      }
    }
  }

  private List<TemplateEntity> getAllTemplatesForGivenIdentifier(String accountId, String orgIdentifier,
      String projectIdentifier, String templateIdentifier, Boolean getDistinctFromBranches) {
    Criteria criteria = templateServiceHelper.formCriteria(accountId, orgIdentifier, projectIdentifier, "",
        TemplateFilterPropertiesDTO.builder()
            .templateIdentifiers(Collections.singletonList(templateIdentifier))
            .build(),
        false, "", false);
    PageRequest pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, TemplateEntityKeys.lastUpdatedAt));
    return list(criteria, pageRequest, accountId, orgIdentifier, projectIdentifier, getDistinctFromBranches)
        .getContent();
  }
}
