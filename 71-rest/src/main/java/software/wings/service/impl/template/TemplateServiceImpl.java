package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.eraro.ErrorCode.TEMPLATES_LINKED;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.template.Template.FOLDER_PATH_ID_KEY;
import static software.wings.beans.template.Template.VERSION_KEY;
import static software.wings.beans.template.TemplateHelper.addUserKeyWords;
import static software.wings.beans.template.TemplateHelper.mappedEntity;
import static software.wings.beans.template.TemplateHelper.obtainTemplateFolderPath;
import static software.wings.beans.template.TemplateHelper.obtainTemplateName;
import static software.wings.beans.template.TemplateType.HTTP;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.beans.template.TemplateVersion.ChangeType.CREATED;
import static software.wings.beans.template.TemplateVersion.TEMPLATE_UUID_KEY;
import static software.wings.beans.template.VersionedTemplate.TEMPLATE_ID_KEY;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.VersionedTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;
import software.wings.utils.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class TemplateServiceImpl implements TemplateService {
  private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Map<String, AbstractTemplateProcessor> templateProcessBinder;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private ExecutorService executorService;
  @Inject private TemplateVersionService templateVersionService;
  @Inject private TemplateHelper templateHelper;
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public PageResponse<Template> list(PageRequest<Template> pageRequest) {
    PageResponse<Template> pageResponse = wingsPersistence.query(Template.class, pageRequest);
    for (Template template : pageResponse.getResponse()) {
      setTemplateDetails(template, null);
    }
    return pageResponse;
  }

  @Override
  @ValidationGroups(Create.class)
  public Template save(Template template) {
    saveOrUpdate(template);
    // create initial version
    processTemplate(template);

    template.setVersion(Long.valueOf(1));
    template.setKeywords(getKeywords(template));

    String templateUuid = Validator.duplicateCheck(() -> wingsPersistence.save(template), "name", template.getName());
    getTemplateVersion(template, templateUuid, template.getType(), template.getName(), CREATED);

    // Save Versioned template
    wingsPersistence.save(buildTemplateDetails(template, templateUuid));

    return get(templateUuid);
  }

  private VersionedTemplate buildTemplateDetails(Template template, String templateUuid) {
    return VersionedTemplate.builder()
        .templateId(templateUuid)
        .accountId(template.getAccountId())
        .version(template.getVersion())
        .templateObject(template.getTemplateObject())
        .galleryId(template.getGalleryId())
        .variables(template.getVariables())
        .build();
  }

  private void saveOrUpdate(Template template) {
    TemplateFolder templateFolder;
    String galleryId = template.getGalleryId();
    if (isEmpty(galleryId)) {
      TemplateGallery templateGallery = templateGalleryService.getByAccount(template.getAccountId());
      notNullCheck("Template gallery does not exist", templateGallery, USER);
      galleryId = templateGallery.getUuid();
    }
    template.setGalleryId(galleryId);
    if (isEmpty(template.getFolderId())) {
      notNullCheck("Template Folder Path", template.getFolderPath());
      templateFolder = templateFolderService.getByFolderPath(template.getAccountId(), template.getFolderPath());
      notNullCheck(template.getFolderPath() + " does not exist", templateFolder, USER);
    } else {
      templateFolder = templateFolderService.get(template.getFolderId());
    }
    notNullCheck("Folder does not exist", templateFolder, USER);
    template.setFolderId(templateFolder.getUuid());
    if (templateFolder.getPathId() == null) {
      template.setFolderPathId(templateFolder.getUuid());
    } else {
      template.setFolderPathId(templateFolder.getPathId() + "/" + templateFolder.getUuid());
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public Template update(Template template) {
    saveOrUpdate(template);
    Template oldTemplate;
    if (template.getVersion() == null) {
      oldTemplate = get(template.getUuid());
      template.setVersion(oldTemplate.getVersion());
    } else {
      oldTemplate = get(template.getAccountId(), template.getUuid(), String.valueOf(template.getVersion()));
    }
    notNullCheck("Template " + template.getName() + " does not exist", oldTemplate);

    VersionedTemplate newVersionedTemplate = buildTemplateDetails(template, template.getUuid());

    DiffNode templateDetailsDiff = ObjectDifferBuilder.buildDefault().compare(
        oldTemplate.getTemplateObject(), newVersionedTemplate.getTemplateObject());

    DiffNode variablesDiff =
        ObjectDifferBuilder.buildDefault().compare(oldTemplate.getVariables(), newVersionedTemplate.getVariables());

    boolean templateDetailsChanged = false;
    if (templateDetailsDiff.hasChanges() || variablesDiff.hasChanges()) {
      TemplateVersion templateVersion = getTemplateVersion(
          template, template.getUuid(), template.getType(), template.getName(), TemplateVersion.ChangeType.UPDATED);
      newVersionedTemplate.setVersion(Long.valueOf(templateVersion.getVersion().intValue()));
      saveVersionedTemplate(template, newVersionedTemplate);
      templateDetailsChanged = true;
    }

    wingsPersistence.save(template);

    Template savedTemplate = get(template.getAccountId(), template.getUuid(), String.valueOf(template.getVersion()));
    if (templateDetailsChanged) {
      executorService.submit(() -> updateLinkedEntities(savedTemplate));
    }
    return savedTemplate;
  }

  private void saveVersionedTemplate(Template template, VersionedTemplate newVersionedTemplate) {
    wingsPersistence.saveAndGet(VersionedTemplate.class, newVersionedTemplate);
    template.setVersion(newVersionedTemplate.getVersion());
  }

  private TemplateVersion getTemplateVersion(
      Template template, String uuid, String templateType, String templateName, TemplateVersion.ChangeType updated) {
    return templateVersionService.newTemplateVersion(
        template.getAccountId(), template.getGalleryId(), uuid, templateType, templateName, updated);
  }

  @Override
  public Template get(String templateUuid) {
    Template template = wingsPersistence.get(Template.class, templateUuid);
    notNullCheck("Template was deleted", template);
    setTemplateDetails(template, null);
    return template;
  }

  @Override
  public Template get(String accountId, String templateId, String version) {
    Query<Template> templateQuery =
        wingsPersistence.createQuery(Template.class).filter(ACCOUNT_ID_KEY, accountId).filter(ID_KEY, templateId);
    return getTemplate(version, templateQuery);
  }

  private Template getTemplate(String version, Query<Template> templateQuery) {
    Template template = templateQuery.get();
    notNullCheck("Template does not exist", template);
    setTemplateDetails(template, version);
    return template;
  }

  @Override
  public Template get(String templateId, String version) {
    Query<Template> templateQuery = wingsPersistence.createQuery(Template.class).filter(ID_KEY, templateId);
    return getTemplate(version, templateQuery);
  }

  private void setTemplateDetails(Template template, String version) {
    Long templateVersion = version == null || version.equals("latest") ? template.getVersion() : Long.valueOf(version);
    VersionedTemplate versionedTemplate =
        getVersionedTemplate(template.getAccountId(), template.getUuid(), templateVersion);
    notNullCheck(
        "Template [" + template.getName() + "] with version [" + version + "] does not exist", versionedTemplate);
    template.setTemplateObject(versionedTemplate.getTemplateObject());
    template.setVersion(templateVersion);
    template.setVariables(versionedTemplate.getVariables());
  }

  @Override
  public VersionedTemplate getVersionedTemplate(String accountId, String templateUuid, Long templateVersion) {
    return wingsPersistence.createQuery(VersionedTemplate.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(TEMPLATE_ID_KEY, templateUuid)
        .filter(VERSION_KEY, templateVersion)
        .get();
  }

  @Override
  public boolean delete(String accountId, String templateUuid) {
    Template template = wingsPersistence.createQuery(Template.class)
                            .filter(ACCOUNT_ID_KEY, accountId)
                            .filter(ID_KEY, templateUuid)
                            .get();

    TemplateType templateType = TemplateType.valueOf(template.getType());
    if (templateHelper.templatesLinked(templateType, Collections.singletonList(templateUuid))) {
      throw new WingsException(TEMPLATES_LINKED, USER)
          .addParam("message", String.format("Template : [%s] couldn't be deleted", template.getName()))
          .addParam("type", mappedEntity(templateType));
    }
    boolean templateDeleted = wingsPersistence.delete(template);
    if (templateDeleted) {
      wingsPersistence.delete(wingsPersistence.createQuery(VersionedTemplate.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(TEMPLATE_ID_KEY, templateUuid));
      wingsPersistence.delete(wingsPersistence.createQuery(TemplateVersion.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(TEMPLATE_UUID_KEY, templateUuid));
    }

    return templateDeleted;
  }

  @Override
  public void loadDefaultTemplates(TemplateType templateType, String accountId, String accountName) {
    logger.info("Loading default templates for template type {}", templateType);
    AbstractTemplateProcessor abstractTemplateProcessor = getTemplateProcessor(templateType.name());
    abstractTemplateProcessor.loadDefaultTemplates(accountId, accountName);
    logger.info("Loading default templates for template type {} success", templateType);
  }

  @Override
  public Template loadYaml(TemplateType templateType, String yamlFilePath, String accountId, String accountName) {
    return getTemplateProcessor(templateType.name()).loadYaml(yamlFilePath, accountId, accountName);
  }

  @Override
  public List<CommandCategory> getCommandCategories(String accountId, String templateId) {
    return templateHelper.getCommandCategories(accountId, templateId);
  }

  @Override
  public TemplateFolder getTemplateTree(String accountId, String keyword, List<String> templateTypes) {
    return templateFolderService.getTemplateTree(accountId, keyword, templateTypes);
  }

  @Override
  public void updateLinkedEntities(Template savedTemplate) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(savedTemplate);
    abstractTemplateProcessor.updateLinkedEntities(savedTemplate);
  }

  @Override
  public boolean deleteByFolder(TemplateFolder templateFolder) {
    List<Key<Template>> templateKeys = wingsPersistence.createQuery(Template.class)
                                           .filter(ACCOUNT_ID_KEY, templateFolder.getAccountId())
                                           .field(FOLDER_PATH_ID_KEY)
                                           .contains(templateFolder.getUuid())
                                           .asKeyList();
    List<String> templateUuids =
        templateKeys.stream().map(templateKey -> templateKey.getId().toString()).collect(Collectors.toList());
    if (isEmpty(templateUuids)) {
      logger.info("No templates under the folder {}", templateFolder.getName());
      return true;
    }
    logger.info("To be deleted linked template uuids {}", templateUuids);
    // Verify if Service Commands contains the given ids
    if (templateHelper.templatesLinked(SSH, templateUuids)) {
      throwException(templateFolder, SERVICE);
    }
    if (templateHelper.templatesLinked(HTTP, templateUuids)) {
      throwException(templateFolder, WORKFLOW);
    }
    // Delete templates
    return wingsPersistence.delete(wingsPersistence.createQuery(Template.class)
                                       .filter(ACCOUNT_ID_KEY, templateFolder.getAccountId())
                                       .field(Template.ID_KEY)
                                       .in(templateUuids));
  }

  @Override
  public String fetchTemplateUri(String templateUuid) {
    StringBuilder templateFolderPath = new StringBuilder("");
    Template template = wingsPersistence.get(Template.class, templateUuid);
    if (template == null) {
      logger.error("Linked template for http template  {} was deleted ", templateUuid);
      return null;
    }
    List<String> folderUuids = Arrays.stream(template.getFolderPathId().split("/")).collect(Collectors.toList());
    Map<String, String> templateUuidNameMap =
        templateFolderService.fetchTemplateFolderNames(template.getAccountId(), folderUuids);
    int i = 0;
    for (String folderId : folderUuids) {
      templateFolderPath = templateFolderPath.append(templateUuidNameMap.get(folderId));
      if (i != folderUuids.size() - 1) {
        templateFolderPath = templateFolderPath.append("/");
      }
      i++;
    }
    return templateFolderPath.append("/").append(template.getName()).toString();
  }

  @Override
  public Object constructEntityFromTemplate(String templateId, String version) {
    Template template = get(templateId, version);
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    return abstractTemplateProcessor.constructEntityFromTemplate(template);
  }

  @Override
  public String fetchTemplateIdFromUri(String accountId, String templateUri) {
    String folderPath = obtainTemplateFolderPath(templateUri);
    TemplateFolder templateFolder = templateFolderService.getByFolderPath(accountId, folderPath);
    if (templateFolder == null) {
      throw new WingsException("No template folder found with the uri  [" + templateUri + "]");
    }

    String templateName = obtainTemplateName(templateUri);
    Template template = wingsPersistence.createQuery(Template.class)
                            .project(Template.NAME_KEY, true)
                            .project(Template.ACCOUNT_ID_KEY, true)
                            .filter(Template.ACCOUNT_ID_KEY, accountId)
                            .filter(Template.NAME_KEY, templateName)
                            .filter(Template.FOLDER_ID_KEY, templateFolder.getUuid())
                            .get();
    if (template == null) {
      throw new WingsException("No template found for the uri [" + templateUri + "]");
    }
    return template.getUuid();
  }

  private void throwException(TemplateFolder templateFolder, EntityType entityType) {
    throw new WingsException(TEMPLATES_LINKED, USER)
        .addParam("message", String.format("Template Folder : [%s] couldn't be deleted", templateFolder.getName()))
        .addParam("type", entityType.name());
  }

  private AbstractTemplateProcessor getAbstractTemplateProcessor(Template template) {
    if (template.getType() != null) {
      return getTemplateProcessor(template.getType());
    } else {
      TemplateType templateType = getTemplateType(template.getTemplateObject());
      template.setType(templateType.name());
      return getTemplateProcessor(templateType.name());
    }
  }

  private void processTemplate(Template template) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    if (abstractTemplateProcessor != null) {
      abstractTemplateProcessor.process(template);
    }
  }

  private TemplateType getTemplateType(BaseTemplate templateObject) {
    if (templateObject instanceof SshCommandTemplate) {
      return SSH;
    } else if (templateObject instanceof HttpTemplate) {
      return HTTP;
    }
    throw new InvalidRequestException("Template Type not yet supported", USER);
  }

  private AbstractTemplateProcessor getTemplateProcessor(String templateType) {
    return templateProcessBinder.get(templateType);
  }

  private List<String> getKeywords(Template template) {
    List<String> generatedKeywords = trimList(template.generateKeywords());
    return addUserKeyWords(template.getKeywords(), generatedKeywords);
  }
}
