package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.VersionedTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.List;

public class SystemTemplateGalleryMigration implements Migration {
  private static final Logger logger = getLogger(SystemTemplateGalleryMigration.class);
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private TemplateService templateService;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public void migrate() {
    logger.info("Migrating Harness Inc Gallery");
    String url = mainConfiguration.getPortal().getUrl();
    if (isEmpty(url)) {
      logger.info("Not running template migrations");
    }
    if ("https://stage.harness.io".equals(url) || "https://app.harness.io".equals(url) || url.contains("stage")
        || url.contains("app")) {
      logger.info("Not running template migrations on Prod and Staging environment");
      return;
    }
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (templateGallery != null) {
      templateGalleryService.delete(templateGallery.getUuid());
    }
    logger.info("Harness template gallery does not exist. Creating one");
    templateGallery = TemplateGallery.builder().name(HARNESS_GALLERY).accountId(GLOBAL_ACCOUNT_ID).build();
    templateGallery.setAppId(GLOBAL_APP_ID);
    templateGalleryService.save(templateGallery);
    logger.info("Harness template gallery created successfully");

    logger.info("Loading Harness default template folders");
    wingsPersistence.delete(
        wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID));
    templateFolderService.loadDefaultTemplateFolders();
    logger.info("Loading Harness default template folders success");
    logger.info("Loading default templates for command");
    wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID));
    wingsPersistence.delete(
        wingsPersistence.createQuery(VersionedTemplate.class).filter(ACCOUNT_ID_KEY, GLOBAL_ACCOUNT_ID));
    templateService.loadDefaultTemplates(TemplateType.SSH, GLOBAL_ACCOUNT_ID);
    logger.info("Loading default templates for command success");

    List<Account> accounts = accountService.list(aPageRequest().addFilter(APP_ID_KEY, EQ, GLOBAL_APP_ID).build());
    accounts.forEach(account -> {
      wingsPersistence.delete(
          wingsPersistence.createQuery(TemplateFolder.class).filter(ACCOUNT_ID_KEY, account.getUuid()));
      templateFolderService.copyHarnessTemplateFolders(account.getUuid());
      wingsPersistence.delete(wingsPersistence.createQuery(Template.class).filter("accountId", account.getUuid()));
      wingsPersistence.delete(
          wingsPersistence.createQuery(VersionedTemplate.class).filter("accountId", account.getUuid()));
      templateService.loadDefaultTemplates(TemplateType.SSH, account.getUuid());
      templateService.loadDefaultTemplates(TemplateType.HTTP, account.getUuid());
    });
  }
}
