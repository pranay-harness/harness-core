package software.wings.service.intfc.template;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.CommandCategory;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.VersionedTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface TemplateService {
  PageResponse<Template> list(PageRequest<Template> pageRequest);

  Template save(@Valid @NotNull Template template);

  Template update(@Valid Template template);

  Template get(String templateId);

  Template get(@NotEmpty String accountId, @NotEmpty String templateId, String version);

  Template get(@NotEmpty String templateId, String version);

  boolean delete(String accountId, String templateUuid);

  void loadDefaultTemplates(TemplateType templateType, @NotEmpty String accountId);

  Template loadYaml(TemplateType templateType, String yamlFilePath, String accountId);

  List<CommandCategory> getCommandCategories(@NotEmpty String accountId, @NotEmpty String templateId);

  TemplateFolder getTemplateTree(@NotEmpty String accountId, String keyword, List<String> templateTypes);

  void updateLinkedEntities(Template updatedTemplate);

  boolean deleteByFolder(@Valid TemplateFolder templateFolder);

  String fetchTemplateUri(@NotEmpty String templateUuid);

  Object constructEntityFromTemplate(@NotEmpty String templateId, String version);

  String fetchTemplateIdFromUri(@NotEmpty String accountId, @NotEmpty String templateUri);

  VersionedTemplate getVersionedTemplate(@NotEmpty String accountId, @NotEmpty String templateUuid, Long version);
}
