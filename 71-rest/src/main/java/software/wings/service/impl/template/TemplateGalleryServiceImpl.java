package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.exception.WingsException.USER;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static org.slf4j.LoggerFactory.getLogger;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.template.TemplateGallery.NAME_KEY;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.data.structure.ListUtils;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
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
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class TemplateGalleryServiceImpl implements TemplateGalleryService {
  private static final Logger logger = getLogger(TemplateGalleryServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private TemplateService templateService;
  @Inject private AccountService accountService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

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
        .filter(ACCOUNT_ID_KEY, accountId)
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
        wingsPersistence.createQuery(TemplateGallery.class).filter(ACCOUNT_ID_KEY, accountId).asList();
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

    List<String> userKeywords = ListUtils.trimStrings(templateGallery.getKeywords());
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
    templateService.loadDefaultTemplates(TemplateType.HTTP, GLOBAL_ACCOUNT_ID, gallery.getName());
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
                                .filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(Template.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(VersionedTemplate.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateVersion.class)
                                .filter(Template.GALLERY_ID_KEY, galleryId)
                                .filter(ACCOUNT_ID_KEY, accountId));
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
    logger.info("Copying default templates for account {} success", accountName);
  }

  private List<String> getKeywords(TemplateGallery templateGallery) {
    List<String> generatedKeywords = trimList(templateGallery.generateKeywords());
    return TemplateHelper.addUserKeyWords(templateGallery.getKeywords(), generatedKeywords);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateGallery.class).filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(VersionedTemplate.class).filter(ACCOUNT_ID_KEY, accountId));
    wingsPersistence.delete(wingsPersistence.createQuery(TemplateVersion.class).filter(ACCOUNT_ID_KEY, accountId));
  }
}
