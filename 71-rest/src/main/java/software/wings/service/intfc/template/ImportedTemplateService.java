package software.wings.service.intfc.template;

import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.ImportedCommand;

import java.util.List;
import java.util.Map;

public interface ImportedTemplateService {
  boolean isImported(String templateId, String accountId);

  String getImportedTemplateVersionFromTemplateVersion(String templateId, String version, String accountId);

  String getTemplateVersionFromImportedTemplateVersion(String templateId, String importedVersion, String accountId);

  List<Template> getTemplatesByCommandNames(List<String> commandNames, String commandStoreName, String accountId);

  Template getTemplateByCommandName(String commandName, String commandStoreName, String accountId);

  ImportedTemplate getCommandByTemplateId(String templateId, String accountId);

  Map<String, Template> getCommandNameTemplateMap(List<String> commandNames, String commandStoreName, String accountId);

  List<ImportedCommand> makeImportedCommandObjectWithLatestVersion(
      Map<String, TemplateVersion> templateUuidLatestVersionMap, List<String> commandNames, String commandStoreName,
      Map<String, Template> commandNameTemplateMap, String accountId);

  ImportedCommand makeImportedCommandObject(String commandId, String commandStoreId,
      List<TemplateVersion> templateVersions, String accountId, Template template);

  ImportedTemplate get(String commandId, String commandStoreId, String accountId);

  Template getAndSaveImportedTemplate(String version, String commandName, String commandStoreName, String accountId);
}
