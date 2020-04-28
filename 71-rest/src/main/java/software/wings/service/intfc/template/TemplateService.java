package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.VersionedTemplate;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedByApplication;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface TemplateService extends OwnedByAccount, OwnedByApplication {
  PageResponse<Template> list(PageRequest<Template> pageRequest, List<String> galleryKeys, String accountId);

  @ValidationGroups(Create.class) Template saveReferenceTemplate(Template template);

  @ValidationGroups(Update.class) Template updateReferenceTemplate(Template template);

  Template save(@Valid @NotNull Template template);

  Template update(@Valid Template template);

  Template get(String templateId);

  Template get(@NotEmpty String accountId, @NotEmpty String templateId, String version);

  Template get(@NotEmpty String templateId, String version);

  boolean delete(String accountId, String templateUuid);

  //  @Deprecated should not be used in future code,
  void loadDefaultTemplates(TemplateType templateType, @NotEmpty String accountId, @NotEmpty String accountName);

  Template loadYaml(TemplateType templateType, String yamlFilePath, String accountId, String accountName);

  List<CommandCategory> getCommandCategories(
      @NotEmpty String accountId, @NotEmpty String appId, @NotEmpty String templateId);

  TemplateFolder getTemplateTree(@NotEmpty String accountId, String keyword, List<String> templateTypes);

  TemplateFolder getTemplateTree(
      @NotEmpty String accountId, @NotEmpty String appId, String keyword, List<String> templateTypes);

  void updateLinkedEntities(Template updatedTemplate);

  boolean deleteByFolder(@Valid TemplateFolder templateFolder);

  String fetchTemplateUri(@NotEmpty String templateUuid);

  String makeNamespacedTemplareUri(String templateUuid, String version);

  Object constructEntityFromTemplate(@NotEmpty String templateId, String version, EntityType entityType);

  Object constructEntityFromTemplate(Template template, EntityType entityType);

  String fetchTemplateIdFromUri(@NotEmpty String accountId, @NotEmpty String templateUri);

  String fetchTemplateIdFromUri(@NotEmpty String accountId, @NotEmpty String appId, @NotEmpty String templateUri);

  String fetchTemplateIdByNameAndFolderId(String accountId, String name, String folderId, String galleryId);

  VersionedTemplate getVersionedTemplate(@NotEmpty String accountId, @NotEmpty String templateUuid, Long version);

  Template fetchTemplateByKeywordForAccountGallery(@NotEmpty String accountId, String keyword);

  Template fetchTemplateByKeywordForAccountGallery(@NotEmpty String accountId, @NotEmpty String appId, String keyword);

  Template fetchTemplateByKeywordsForAccountGallery(@NotEmpty String accountId, Set<String> keywords);

  List<Template> fetchTemplatesWithReferencedTemplateId(@NotEmpty String templateId);

  Template convertYamlToTemplate(String templatePath) throws IOException;

  void loadDefaultTemplates(List<String> templateFiles, String accountId, String accountName);

  List<String> fetchTemplateProperties(Template template);

  Template findByFolder(TemplateFolder templateFolder, String templateName, String appId);

  Template fetchTemplateFromUri(String templateUri, String accountId, String appId);

  String fetchTemplateVersionFromVersion(String templateUuid, String templateVersion);

  String fetchTemplateVersionFromUri(String templateUuid, String templateUri);

  String getTemplateFolderPathString(Template template);
}
