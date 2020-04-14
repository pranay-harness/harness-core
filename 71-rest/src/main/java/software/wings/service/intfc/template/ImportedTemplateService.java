package software.wings.service.intfc.template;

import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.ImportedCommand;

import java.util.List;
import java.util.Map;

public interface ImportedTemplateService {
  boolean isImported(String templateId, String accountId);

  List<Template> getTemplatesByCommandIds(List<String> commandIds, String commandStoreId, String accountId);

  Template getTemplateByCommandId(String commandId, String commandStoreId, String accountId);

  ImportedTemplate getCommandByTemplateId(String templateId, String accountId);

  Map<String, Template> getCommandIdTemplateMap(List<String> commandIds, String commandStoreId, String accountId);

  List<ImportedCommand> makeImportedCommandObjectWithLatestVersion(
      Map<String, TemplateVersion> templateUuidLatestVersionMap, List<String> commandIds, String commandStoreId,
      Map<String, Template> commandIdTemplateMap, String accountId);

  ImportedCommand makeImportedCommandObject(String commandId, String commandStoreId,
      List<TemplateVersion> templateVersions, String accountId, Template template);

  ImportedTemplate get(String commandId, String commandStoreId, String accountId);

  Template getAndSaveImportedTemplate(
      String token, String version, String commandId, String commandStoreId, String accountId);
}
