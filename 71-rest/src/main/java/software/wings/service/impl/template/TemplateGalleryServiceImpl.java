package software.wings.service.impl.template;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.PersistenceValidator.duplicateCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateGallery.NAME_KEY;
import static software.wings.common.TemplateConstants.GENERIC_JSON_PATH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.PATH_DELIMITER;
import static software.wings.common.TemplateConstants.POWER_SHELL_COMMANDS;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_APP_V4_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_WEBSITE_V4_INSTALL_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_START_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_STOP_PATH;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.VersionedTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class TemplateGalleryServiceImpl implements TemplateGalleryService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private TemplateService templateService;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private AccountService accountService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public PageResponse<TemplateGallery> list(PageRequest<TemplateGallery> pageRequest) {
    return wingsPersistence.query(TemplateGallery.class, pageRequest);
  }

  @Override
  @ValidationGroups(Create.class)
  public TemplateGallery save(TemplateGallery templateGallery) {
    templateGallery.setKeywords(getKeywords(templateGallery));
    TemplateGallery finalTemplateGallery = templateGallery;
    return duplicateCheck(()
                              -> wingsPersistence.saveAndGet(TemplateGallery.class, finalTemplateGallery),
        NAME_KEY, templateGallery.getName());
  }

  @Override
  public TemplateGallery get(String accountId, String galleryName) {
    return wingsPersistence.createQuery(TemplateGallery.class)
        .filter(TemplateGallery.ACCOUNT_ID_KEY, accountId)
        .filter(NAME_KEY, galleryName.trim())
        .get();
  }

  @Override
  public TemplateGallery get(String uuid) {
    return wingsPersistence.get(TemplateGallery.class, uuid);
  }

  @Override
  public TemplateGallery getByAccount(String accountId) {
    List<TemplateGallery> templateGalleries =
        wingsPersistence.createQuery(TemplateGallery.class).filter(TemplateGallery.ACCOUNT_ID_KEY, accountId).asList();
    if (isNotEmpty(templateGalleries)) {
      return templateGalleries.get(0);
    }
    return null;
  }

  @Override
  @ValidationGroups(Update.class)
  public TemplateGallery update(TemplateGallery templateGallery) {
    TemplateGallery savedGallery = get(templateGallery.getUuid());
    notNullCheck("Template Gallery [" + templateGallery.getName() + "] was deleted", savedGallery, USER);

    Query<TemplateGallery> query =
        wingsPersistence.createQuery(TemplateGallery.class).field(ID_KEY).equal(templateGallery.getUuid());
    UpdateOperations<TemplateGallery> operations = wingsPersistence.createUpdateOperations(TemplateGallery.class);

    Set<String> userKeywords = trimmedLowercaseSet(templateGallery.getKeywords());
    if (isNotEmpty(templateGallery.getDescription())) {
      if (isNotEmpty(userKeywords)) {
        userKeywords.remove(savedGallery.getDescription().toLowerCase());
      }
      operations.set("description", templateGallery.getDescription());
    }
    operations.set("keywords", getKeywords(templateGallery));
    wingsPersistence.update(query, operations);
    return get(savedGallery.getUuid());
  }

  @Override
  public void delete(String galleryUuid) {
    TemplateGallery templateGallery = get(galleryUuid);
    if (templateGallery == null) {
      return;
    }
    deleteGalleryContents(templateGallery.getAccountId(), templateGallery.getUuid());
    wingsPersistence.delete(TemplateGallery.class, templateGallery.getUuid());
  }

  @Override
  public void loadHarnessGallery() {
    logger.info("Loading Harness Inc Gallery");
    deleteAccountGalleryByName(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    logger.info("Creating harness gallery");
    TemplateGallery gallery = saveHarnessGallery();
    logger.info("Harness template gallery created successfully");
    logger.info("Loading Harness default template folders");
    templateFolderService.loadDefaultTemplateFolders();
    logger.info("Loading Harness default template folders success");
    logger.info("Loading default templates for command");
    templateService.loadDefaultTemplates(TemplateType.SSH, GLOBAL_ACCOUNT_ID, gallery.getName());
    logger.info("Loading default templates for command success");
    logger.info("Loading default templates for http");
    templateService.loadDefaultTemplates(TemplateType.HTTP, GLOBAL_ACCOUNT_ID, gallery.getName());
    logger.info("Loading default templates for http success");
    logger.info("Loading default templates for shell script");
    templateService.loadDefaultTemplates(TemplateType.SHELL_SCRIPT, GLOBAL_ACCOUNT_ID, gallery.getName());
    logger.info("Loading default templates for shell script success");
  }

  public TemplateGallery saveHarnessGallery() {
    return wingsPersistence.saveAndGet(TemplateGallery.class,
        TemplateGallery.builder()
            .name(HARNESS_GALLERY)
            .description("Harness gallery")
            .accountId(GLOBAL_ACCOUNT_ID)
            .global(true)
            .appId(GLOBAL_APP_ID)
            .build());
  }

  public void copyHarnessTemplates() {
    List<Account> accounts = accountService.listAllAccounts();
    for (Account account : accounts) {
      if (!GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
        deleteByAccountId(account.getUuid());
        copyHarnessTemplatesToAccount(account.getUuid(), account.getAccountName());
      }
    }
  }

  @Override
  public void deleteAccountGalleryByName(String accountId, String galleryName) {
    TemplateGallery accountGallery = get(accountId, galleryName);
    if (accountGallery != null) {
      deleteGalleryContents(accountId, accountGallery.getUuid());
      wingsPersistence.delete(TemplateGallery.class, accountGallery.getUuid());
    }
  }

  private void deleteGalleryContents(String accountId, String galleryId) {
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateFolder.class)
                                .filter(TemplateFolder.GALLERY_ID_KEY, galleryId)
                                .filter(TemplateFolder.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(Template.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(TemplateFolder.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(VersionedTemplate.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(TemplateFolder.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateVersion.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(TemplateFolder.ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public void copyHarnessTemplatesToAccount(String accountId, String accountName) {
    logger.info("Copying Harness templates for the account {}", accountName);

    TemplateGallery harnessTemplateGallery = get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }
    logger.info("Creating Account gallery");
    TemplateGallery accountGallery = save(TemplateGallery.builder()
                                              .name(accountName)
                                              .appId(GLOBAL_APP_ID)
                                              .accountId(accountId)
                                              .referencedGalleryId(harnessTemplateGallery.getUuid())
                                              .build());
    logger.info("Creating Account gallery success");
    logger.info("Copying harness template folders to account {}", accountName);
    templateFolderService.copyHarnessTemplateFolders(accountGallery.getUuid(), accountId, accountName);
    logger.info("Copying harness template folders to account {} success", accountName);
    logger.info("Copying default templates for account {}", accountName);
    templateService.loadDefaultTemplates(TemplateType.SSH, accountId, accountName);
    templateService.loadDefaultTemplates(TemplateType.HTTP, accountId, accountName);
    //    templateService.loadDefaultTemplates(TemplateType.SHELL_SCRIPT, accountId, accountName);
    logger.info("Copying default templates for account {} success", accountName);
  }

  public void copyHarnessTemplatesToAccountV2(String accountId, String accountName) {
    logger.info("Copying Harness templates for the account {}", accountName);

    TemplateGallery harnessTemplateGallery = get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }
    logger.info("Creating Account gallery");
    TemplateGallery accountGallery = save(TemplateGallery.builder()
                                              .name(accountName)
                                              .appId(GLOBAL_APP_ID)
                                              .accountId(accountId)
                                              .referencedGalleryId(harnessTemplateGallery.getUuid())
                                              .build());
    logger.info("Creating Account gallery success");
    logger.info("Copying harness template folders to account {}", accountName);
    templateFolderService.copyHarnessTemplateFolders(accountGallery.getUuid(), accountId, accountName);
    logger.info("Copying harness template folders to account {} success", accountName);
    logger.info("Copying default templates for account {}", accountName);
    //    templateService.loadDefaultTemplates(TemplateType.SSH, accountId, accountName);
    templateService.loadDefaultTemplates(TemplateType.HTTP, accountId, accountName);
    templateService.loadDefaultTemplates(
        asList(TOMCAT_WAR_STOP_PATH, TOMCAT_WAR_START_PATH, TOMCAT_WAR_INSTALL_PATH, GENERIC_JSON_PATH,
            POWER_SHELL_IIS_WEBSITE_V4_INSTALL_PATH, POWER_SHELL_IIS_APP_V4_INSTALL_PATH),
        accountId, accountName);
    templateGalleryService.copyHarnessTemplateFromGalleryToAccount(POWER_SHELL_COMMANDS, TemplateType.SSH,
        "Install IIS Application", POWER_SHELL_IIS_APP_V4_INSTALL_PATH, accountId, accountName);
    templateGalleryService.copyHarnessTemplateFromGalleryToAccount(POWER_SHELL_COMMANDS, TemplateType.SSH,
        "Install IIS Website", POWER_SHELL_IIS_WEBSITE_V4_INSTALL_PATH, accountId, accountName);
    logger.info("Copying default templates for account {} success", accountName);
  }
  private Set<String> getKeywords(TemplateGallery templateGallery) {
    Set<String> generatedKeywords = trimmedLowercaseSet(templateGallery.generateKeywords());
    return TemplateHelper.addUserKeyWords(templateGallery.getKeywords(), generatedKeywords);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateGallery.class).filter(TemplateGallery.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateFolder.class).filter(TemplateFolder.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter(Template.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(
        wingsPersistence.createQuery(VersionedTemplate.class).filter(VersionedTemplate.ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateVersion.class).filter(TemplateVersion.ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public void copyHarnessTemplateFromGalleryToAccounts(
      String sourceFolderPath, TemplateType templateType, String templateName, String yamlFilePath) {
    logger.info("Copying Harness template [{}] from global account to all accounts", templateName);

    TemplateGallery harnessTemplateGallery = get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }

    List<Account> accounts = accountService.listAllAccounts();
    for (Account account : accounts) {
      try {
        logger.info("Copying template [{}] started for account [{}]", templateName, account.getUuid());
        boolean templateGalleryExists = wingsPersistence.createQuery(TemplateGallery.class)
                                            .field(TemplateGallery.NAME_KEY)
                                            .equal(account.getAccountName())
                                            .field("accountId")
                                            .equal(account.getUuid())
                                            .getKey()
            != null;
        if (templateGalleryExists) {
          if (!GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
            TemplateFolder destTemplateFolder = templateFolderService.getByFolderPath(
                account.getUuid(), account.getAccountName() + "/" + sourceFolderPath);
            if (destTemplateFolder != null) {
              templateService.loadYaml(templateType, yamlFilePath, account.getUuid(), account.getAccountName());
              logger.info("Template copied to account [{}]", account.getUuid());
            }
          }
        } else {
          logger.info("Template gallery does not exist for account [{}]. Do nothing", account.getUuid());
        }
      } catch (Exception ex) {
        logger.error("Copy Harness template failed for account [{}]", account.getUuid(), ex);
      }
    }
  }

  public void copyHarnessTemplateFromGalleryToAccount(String sourceFolderPath, TemplateType templateType,
      String templateName, String yamlFilePath, String accountId, String accountName) {
    logger.info("Copying Harness template [{}] from global account to all accounts", templateName);

    TemplateGallery harnessTemplateGallery = get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }

    try {
      logger.info("Copying template [{}] started for account [{}]", templateName, accountId);
      boolean templateGalleryExists = wingsPersistence.createQuery(TemplateGallery.class)
                                          .field(TemplateGallery.NAME_KEY)
                                          .equal(accountName)
                                          .field("accountId")
                                          .equal(accountId)
                                          .getKey()
          != null;
      if (templateGalleryExists) {
        if (!GLOBAL_ACCOUNT_ID.equals(accountId)) {
          TemplateFolder destTemplateFolder =
              templateFolderService.getByFolderPath(accountId, accountId + "/" + sourceFolderPath);
          if (destTemplateFolder != null) {
            templateService.loadYaml(templateType, yamlFilePath, accountId, accountName);
          }
          logger.info("Template copied to account [{}]", accountId);
        }
      } else {
        logger.info("Template gallery does not exist for account [{}]. Do nothing", accountId);
      }
    } catch (Exception ex) {
      logger.error("Copy Harness template failed for account [{}]", accountId, ex);
    }
  }

  public void copyNewVersionFromGlobalToAllAccounts(Template globalTemplate, String keyword) {
    List<Account> accounts = accountService.listAllAccounts();
    for (Account account : accounts) {
      if (!GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
        Template existingTemplate = templateService.fetchTemplateByKeyword(account.getUuid(), keyword);
        if (existingTemplate != null) {
          existingTemplate.setReferencedTemplateVersion(globalTemplate.getVersion());
          existingTemplate.setReferencedTemplateId(globalTemplate.getUuid());
          existingTemplate.setTemplateObject(globalTemplate.getTemplateObject());
          existingTemplate.setVariables(globalTemplate.getVariables());
          logger.info("Updating template in account [{}]", account.getUuid());
          templateService.update(existingTemplate);
          logger.info("Template updated in account [{}]", account.getUuid());
        } else {
          logger.info("Template gallery does not exist for account id: [{}] and name:[{}] . Do nothing",
              account.getUuid(), account.getAccountName());
        }
      }
    }
  }

  @Override
  public void copyNewFolderAndTemplatesFromGlobalToAccounts(
      String sourceFolderPath, TemplateType templateType, List<String> yamlFilePaths) {
    logger.info("Trying to copy new folder and templates from global account to all accounts");

    TemplateGallery harnessTemplateGallery = get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (harnessTemplateGallery == null) {
      logger.info("Harness global gallery does not exist. Not copying templates");
      return;
    }

    List<Account> accounts = accountService.listAllAccounts();
    for (Account account : accounts) {
      try {
        logger.info("Copying templates started for account [{}]", account.getUuid());
        TemplateGallery templateGallery = get(account.getUuid(), account.getAccountName());
        if (templateGallery != null) {
          TemplateFolder templateFolder =
              templateFolderService.getRootLevelFolder(account.getUuid(), templateGallery.getUuid());
          if (templateFolder != null) {
            TemplateFolder newTemplateFolder = constructTemplateBuilder(templateFolder, sourceFolderPath);
            TemplateFolder destTemplateFolder = templateFolderService.save(newTemplateFolder);
            if (destTemplateFolder != null) {
              for (String path : yamlFilePaths) {
                templateService.loadYaml(templateType, path, account.getUuid(), account.getAccountName());
              }
            } else {
              logger.info("Folder [{}] could not be added to account. Not copying templates", sourceFolderPath);
            }
          } else {
            logger.info(
                "Parent Template folder does not exist for account [{}]. Cannot proceed with new Folder creation",
                account.getUuid());
          }
        } else {
          logger.info("Template gallery does not exist for account [{}]. Do nothing.", account.getUuid());
        }
      } catch (Exception ex) {
        logger.error("Copy Harness template failed for account [{}]", account.getUuid(), ex);
      }
    }
  }

  private TemplateFolder constructTemplateBuilder(TemplateFolder parentFolder, String folderName) {
    String pathId = parentFolder.getPathId() == null
        ? parentFolder.getUuid()
        : parentFolder.getPathId() + PATH_DELIMITER + parentFolder.getUuid();
    return TemplateFolder.builder()
        .name(folderName)
        .parentId(parentFolder.getUuid())
        .galleryId(parentFolder.getGalleryId())
        .pathId(pathId)
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }
}
