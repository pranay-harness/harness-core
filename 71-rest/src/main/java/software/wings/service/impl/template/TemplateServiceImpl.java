package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.eraro.ErrorCode.TEMPLATES_LINKED;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Account.ACCOUNT_ID_KEY;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.APP_ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.EntityType.ARTIFACT_STREAM;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.template.Template.FOLDER_PATH_ID_KEY;
import static software.wings.beans.template.Template.NAME_KEY;
import static software.wings.beans.template.Template.VERSION_KEY;
import static software.wings.beans.template.TemplateHelper.addUserKeyWords;
import static software.wings.beans.template.TemplateHelper.mappedEntity;
import static software.wings.beans.template.TemplateHelper.obtainTemplateFolderPath;
import static software.wings.beans.template.TemplateHelper.obtainTemplateName;
import static software.wings.beans.template.TemplateType.ARTIFACT_SOURCE;
import static software.wings.beans.template.TemplateType.HTTP;
import static software.wings.beans.template.TemplateType.SHELL_SCRIPT;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.beans.template.TemplateVersion.ChangeType.CREATED;
import static software.wings.beans.template.TemplateVersion.TEMPLATE_UUID_KEY;
import static software.wings.beans.template.VersionedTemplate.TEMPLATE_ID_KEY;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
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
import software.wings.beans.template.VersionedTemplate.VersionedTemplateBuilder;
import software.wings.beans.template.artifactsource.ArtifactSourceTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;
import software.wings.utils.Validator;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class TemplateServiceImpl implements TemplateService {
  private static final String ACCOUNT = "Account";
  private static final String APPLICATION = "Application";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Map<String, AbstractTemplateProcessor> templateProcessBinder;
  @Inject private TemplateFolderService templateFolderService;
  @Inject private ExecutorService executorService;
  @Inject private TemplateVersionService templateVersionService;
  @Inject private TemplateHelper templateHelper;
  @Inject private TemplateGalleryService templateGalleryService;
  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

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

    String templateUuid = Validator.duplicateCheck(() -> wingsPersistence.save(template), NAME_KEY, template.getName());
    getTemplateVersion(template, templateUuid, template.getType(), template.getName(), CREATED);

    // Save Versioned template
    wingsPersistence.save(buildTemplateDetails(template, templateUuid));

    return get(templateUuid);
  }

  private VersionedTemplate buildTemplateDetails(Template template, String templateUuid) {
    VersionedTemplateBuilder builder = VersionedTemplate.builder();
    builder.templateId(templateUuid)
        .accountId(template.getAccountId())
        .version(template.getVersion())
        .templateObject(template.getTemplateObject())
        .galleryId(template.getGalleryId())
        .variables(template.getVariables());
    return builder.build();
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
    processTemplate(template);
    Template oldTemplate;
    if (template.getVersion() == null) {
      oldTemplate = get(template.getUuid());
      template.setVersion(oldTemplate.getVersion());
    } else {
      oldTemplate = get(template.getAccountId(), template.getUuid(), String.valueOf(template.getVersion()));
    }
    notNullCheck("Template " + template.getName() + " does not exist", oldTemplate);
    validateScope(template, oldTemplate);
    List<String> existingKeywords = oldTemplate.getKeywords();
    List<String> generatedKeywords = trimStrings(template.generateKeywords());
    if (isNotEmpty(existingKeywords)) {
      generatedKeywords.addAll(existingKeywords);
    }
    generatedKeywords.remove(oldTemplate.getName().toLowerCase());
    if (oldTemplate.getDescription() != null) {
      generatedKeywords.remove(oldTemplate.getDescription().toLowerCase());
    }
    generatedKeywords.remove(oldTemplate.getType().toLowerCase());
    template.setKeywords(trimList(Arrays.asList(generatedKeywords.toArray())));
    VersionedTemplate newVersionedTemplate = buildTemplateDetails(template, template.getUuid());

    boolean templateObjectChanged = checkTemplateDetailsChanged(
        template, oldTemplate.getTemplateObject(), newVersionedTemplate.getTemplateObject());

    boolean templateVariablesChanged =
        templateHelper.variablesChanged(newVersionedTemplate.getVariables(), oldTemplate.getVariables());

    boolean templateDetailsChanged = false;
    if (templateObjectChanged || templateVariablesChanged) {
      TemplateVersion templateVersion = getTemplateVersion(
          template, template.getUuid(), template.getType(), template.getName(), TemplateVersion.ChangeType.UPDATED);
      newVersionedTemplate.setVersion(Long.valueOf(templateVersion.getVersion().intValue()));
      saveVersionedTemplate(template, newVersionedTemplate);
      templateDetailsChanged = true;
    }

    Validator.duplicateCheck(() -> wingsPersistence.save(template), NAME_KEY, template.getName());

    Template savedTemplate = get(template.getAccountId(), template.getUuid(), String.valueOf(template.getVersion()));
    if (templateDetailsChanged) {
      executorService.submit(() -> updateLinkedEntities(savedTemplate));
    }
    return savedTemplate;
  }

  private void validateScope(Template template, Template oldTemplate) {
    if (!template.getAppId().equals(oldTemplate.getAppId())) {
      String fromScope = oldTemplate.getAppId().equals(GLOBAL_APP_ID) ? ACCOUNT : APPLICATION;
      String toScope = fromScope.equals(APPLICATION) ? ACCOUNT : APPLICATION;
      throw new InvalidRequestException(
          format("Template %s cannot be moved from %s to %s", oldTemplate.getName(), fromScope, toScope));
    }
  }

  private boolean checkTemplateDetailsChanged(Template template, BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    return abstractTemplateProcessor.checkTemplateDetailsChanged(oldTemplate, newTemplate);
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
    Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                        .filter(Template.ACCOUNT_ID_KEY, accountId)
                                        .filter(ID_KEY, templateId);
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
        .filter(VersionedTemplate.ACCOUNT_ID_KEY, accountId)
        .filter(TEMPLATE_ID_KEY, templateUuid)
        .filter(VERSION_KEY, templateVersion)
        .get();
  }

  @Override
  public boolean delete(String accountId, String templateUuid) {
    Template template = wingsPersistence.createQuery(Template.class)
                            .filter(Template.ACCOUNT_ID_KEY, accountId)
                            .filter(ID_KEY, templateUuid)
                            .get();

    TemplateType templateType = TemplateType.valueOf(template.getType());
    if (templateHelper.templatesLinked(templateType, Collections.singletonList(templateUuid))) {
      throw new WingsException(TEMPLATES_LINKED, USER)
          .addParam("message", String.format("Template : [%s] couldn't be deleted", template.getName()))
          .addParam("templateType", templateType.name())
          .addParam("entityType", mappedEntity(templateType));
    }
    boolean templateDeleted = wingsPersistence.delete(template);
    if (templateDeleted) {
      wingsPersistence.delete(wingsPersistence.createQuery(VersionedTemplate.class)
                                  .filter(VersionedTemplate.ACCOUNT_ID_KEY, accountId)
                                  .filter(TEMPLATE_ID_KEY, templateUuid));
      wingsPersistence.delete(wingsPersistence.createQuery(TemplateVersion.class)
                                  .filter(TemplateVersion.ACCOUNT_ID_KEY, accountId)
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
  public List<CommandCategory> getCommandCategories(String accountId, String appId, String templateId) {
    return templateHelper.getCommandCategories(accountId, appId, templateId);
  }

  @Override
  public TemplateFolder getTemplateTree(String accountId, String keyword, List<String> templateTypes) {
    return templateFolderService.getTemplateTree(accountId, keyword, templateTypes);
  }

  @Override
  public TemplateFolder getTemplateTree(String accountId, String appId, String keyword, List<String> templateTypes) {
    return templateFolderService.getTemplateTree(accountId, appId, keyword, templateTypes);
  }

  @Override
  public void updateLinkedEntities(Template savedTemplate) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(savedTemplate);
    abstractTemplateProcessor.updateLinkedEntities(savedTemplate);
  }

  @Override
  public boolean deleteByFolder(TemplateFolder templateFolder) {
    List<Key<Template>> templateKeys = wingsPersistence.createQuery(Template.class)
                                           .filter(Template.ACCOUNT_ID_KEY, templateFolder.getAccountId())
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
      throwException(templateFolder, SSH, SERVICE);
    }
    if (templateHelper.templatesLinked(HTTP, templateUuids)) {
      throwException(templateFolder, HTTP, WORKFLOW);
    }
    if (templateHelper.templatesLinked(SHELL_SCRIPT, templateUuids)) {
      throwException(templateFolder, SHELL_SCRIPT, WORKFLOW);
    }
    if (templateHelper.templatesLinked(ARTIFACT_SOURCE, templateUuids)) {
      throwException(templateFolder, ARTIFACT_SOURCE, ARTIFACT_STREAM);
    }
    // Delete templates
    return wingsPersistence.delete(wingsPersistence.createQuery(Template.class)
                                       .filter(Template.ACCOUNT_ID_KEY, templateFolder.getAccountId())
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
  public Object constructEntityFromTemplate(String templateId, String version, EntityType entityType) {
    Template template = get(templateId, version);
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    return abstractTemplateProcessor.constructEntityFromTemplate(template, entityType);
  }

  @Override
  public Object constructEntityFromTemplate(Template template, EntityType entityType) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    return abstractTemplateProcessor.constructEntityFromTemplate(template, entityType);
  }

  @Override
  public String fetchTemplateIdFromUri(String accountId, String templateUri) {
    return fetchTemplateIdFromUri(accountId, GLOBAL_APP_ID, templateUri);
  }

  @Override
  public String fetchTemplateIdFromUri(String accountId, String appId, String templateUri) {
    String folderPath = obtainTemplateFolderPath(templateUri);
    TemplateFolder templateFolder;
    if (folderPath.contains("/")) { // app level folder
      templateFolder = templateFolderService.getByFolderPath(accountId, appId, folderPath);
    } else { // root level folder
      templateFolder = templateFolderService.getByFolderPath(accountId, folderPath);
    }

    if (templateFolder == null) {
      throw new WingsException("No template folder found with the uri  [" + templateUri + "]");
    }

    String templateName = obtainTemplateName(templateUri);
    Template template = wingsPersistence.createQuery(Template.class)
                            .project(NAME_KEY, true)
                            .project(Template.ACCOUNT_ID_KEY, true)
                            .filter(Template.ACCOUNT_ID_KEY, accountId)
                            .filter(NAME_KEY, templateName)
                            .filter(Template.FOLDER_ID_KEY, templateFolder.getUuid())
                            .filter(APP_ID_KEY, appId)
                            .get();
    if (template == null) {
      throw new WingsException("No template found for the uri [" + templateUri + "]");
    }
    return template.getUuid();
  }

  @Override
  public String fetchTemplateIdByNameAndFolderId(String accountId, String name, String folderId) {
    Template template = wingsPersistence.createQuery(Template.class)
                            .project(NAME_KEY, true)
                            .project(Template.ACCOUNT_ID_KEY, true)
                            .filter(Template.ACCOUNT_ID_KEY, accountId)
                            .filter(NAME_KEY, name)
                            .filter(Template.FOLDER_ID_KEY, folderId)
                            .get();
    if (template == null) {
      throw new WingsException("No template found with name [" + name + "]");
    }
    return template.getUuid();
  }

  private void throwException(TemplateFolder templateFolder, TemplateType templateType, EntityType entityType) {
    throw new WingsException(TEMPLATES_LINKED, USER)
        .addParam("message", String.format("Template Folder : [%s] couldn't be deleted", templateFolder.getName()))
        .addParam("templateType", templateType.name())
        .addParam("entityType", entityType.name());
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
    } else if (templateObject instanceof ShellScriptTemplate) {
      return SHELL_SCRIPT;
    } else if (templateObject instanceof ArtifactSourceTemplate) {
      return ARTIFACT_SOURCE;
    }
    throw new InvalidRequestException("Template Type not yet supported", USER);
  }

  private AbstractTemplateProcessor getTemplateProcessor(String templateType) {
    return templateProcessBinder.get(templateType);
  }

  private List<String> getKeywords(Template template) {
    List<String> generatedKeywords = trimStrings(template.generateKeywords());
    return addUserKeyWords(template.getKeywords(), generatedKeywords);
  }

  public Template fetchTemplateByKeyword(@NotEmpty String accountId, String keyword) {
    Template template = null;
    if (isNotEmpty(keyword)) {
      Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                          .filter(ACCOUNT_ID_KEY, accountId)
                                          .filter(APP_ID_KEY, GLOBAL_APP_ID)
                                          .field(Template.KEYWORDS_KEY)
                                          .in(asList(keyword.toLowerCase()));
      List<Template> templates = templateQuery.asList();
      if (isNotEmpty(templates)) {
        template = templates.get(0);
      }
      if (template != null) {
        setTemplateDetails(template, null);
      }
    }
    return template;
  }

  public Template fetchTemplateByKeyword(@NotEmpty String accountId, @NotEmpty String appId, String keyword) {
    Template template = null;
    if (isNotEmpty(keyword)) {
      Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                          .filter(ACCOUNT_ID_KEY, accountId)
                                          .filter(APP_ID_KEY, appId)
                                          .field(Template.KEYWORDS_KEY)
                                          .in(asList(keyword.toLowerCase()));
      List<Template> templates = templateQuery.asList();
      if (isNotEmpty(templates)) {
        template = templates.get(0);
      }
      if (template != null) {
        setTemplateDetails(template, null);
      }
    }
    return template;
  }

  public Template convertYamlToTemplate(String templatePath) throws IOException {
    URL url = this.getClass().getClassLoader().getResource(templatePath);
    return mapper.readValue(url, Template.class);
  }

  public void loadDefaultTemplates(List<String> templateFiles, String accountId, String accountName) {
    // First
    templateFiles.forEach(templatePath -> {
      try {
        logger.info("Loading url file {} for the account {} ", templatePath, accountId);
        loadAndSaveTemplate(templatePath, accountId, accountName);
      } catch (WingsException exception) {
        String msg = "Failed to save template from file [" + templatePath + "] for the account [" + accountId
            + "] . Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      } catch (IOException exception) {
        String msg = "Failed to save template from file [" + templatePath + "]. Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      }
    });
  }

  private Template loadAndSaveTemplate(String templatePath, String accountId, String accountName) throws IOException {
    URL url = this.getClass().getClassLoader().getResource(templatePath);
    Template template = mapper.readValue(url, Template.class);

    if (!GLOBAL_ACCOUNT_ID.equals(accountId)) {
      String referencedTemplateUri = template.getReferencedTemplateUri();
      if (isNotEmpty(referencedTemplateUri)) {
        String referencedTemplateVersion = TemplateHelper.obtainTemplateVersion(referencedTemplateUri);
        template.setReferencedTemplateId(fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, referencedTemplateUri));
        if (!LATEST_TAG.equals(referencedTemplateVersion)) {
          if (referencedTemplateVersion != null) {
            template.setReferencedTemplateVersion(Long.parseLong(referencedTemplateVersion));
          }
        }
      }
      if (isNotEmpty(template.getFolderPath())) {
        template.setFolderPath(template.getFolderPath().replace(HARNESS_GALLERY, accountName));
      }
    }
    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(accountId);
    return save(template);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<Template> templates =
        wingsPersistence.createQuery(Template.class).filter(Template.ACCOUNT_ID_KEY, accountId).asList();
    for (Template template : templates) {
      delete(accountId, template.getUuid());
    }
  }

  public List<String> fetchTemplateProperties(Template template) {
    AbstractTemplateProcessor abstractTemplateProcessor = getAbstractTemplateProcessor(template);
    return abstractTemplateProcessor.fetchTemplateProperties();
  }
}
