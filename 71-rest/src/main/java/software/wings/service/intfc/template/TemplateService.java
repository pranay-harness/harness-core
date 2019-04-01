package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.CommandCategory;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.VersionedTemplate;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface TemplateService extends OwnedByAccount {
  PageResponse<Template> list(PageRequest<Template> pageRequest);

  Template save(@Valid @NotNull Template template);

  Template update(@Valid Template template);

  Template get(String templateId);

  Template get(@NotEmpty String accountId, @NotEmpty String templateId, String version);

  Template get(@NotEmpty String templateId, String version);

  boolean delete(String accountId, String templateUuid);

  void loadDefaultTemplates(TemplateType templateType, @NotEmpty String accountId, @NotEmpty String accountName);

  Template loadYaml(TemplateType templateType, String yamlFilePath, String accountId, String accountName);

  List<CommandCategory> getCommandCategories(@NotEmpty String accountId, @NotEmpty String templateId);

  TemplateFolder getTemplateTree(@NotEmpty String accountId, String keyword, List<String> templateTypes);

  TemplateFolder getTemplateTree(
      @NotEmpty String accountId, @NotEmpty String appId, String keyword, List<String> templateTypes);

  void updateLinkedEntities(Template updatedTemplate);

  boolean deleteByFolder(@Valid TemplateFolder templateFolder);

  String fetchTemplateUri(@NotEmpty String templateUuid);

  Object constructEntityFromTemplate(@NotEmpty String templateId, String version);

  String fetchTemplateIdFromUri(@NotEmpty String accountId, @NotEmpty String templateUri);

  String fetchTemplateIdByNameAndFolderId(String accountId, String name, String folderId);

  VersionedTemplate getVersionedTemplate(@NotEmpty String accountId, @NotEmpty String templateUuid, Long version);

  Template fetchTemplateByKeyword(@NotEmpty String accountId, String keyword);

  Template convertYamlToTemplate(String templatePath) throws IOException;

  void loadDefaultTemplates(List<String> templateFiles, String accountId, String accountName);

  List<String> fetchTemplateProperties(@NotEmpty String templateId, String version);
}
