/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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

  List<Template> getTemplatesByCommandNames(
      List<String> commandNames, String commandStoreName, String accountId, String appId);

  Template getTemplateByCommandName(String commandName, String commandStoreName, String accountId, String appId);

  ImportedTemplate getCommandByTemplateId(String templateId, String accountId);

  Map<String, Template> getCommandNameTemplateMap(
      List<String> commandNames, String commandStoreName, String accountId, String appId);

  List<ImportedCommand> makeImportedCommandObjectWithLatestVersion(
      Map<String, TemplateVersion> templateUuidLatestVersionMap, List<String> commandNames, String commandStoreName,
      Map<String, Template> commandNameTemplateMap, String accountId);

  ImportedCommand makeImportedCommandObject(String commandId, String commandStoreId,
      List<TemplateVersion> templateVersions, String accountId, Template template);

  ImportedTemplate get(String commandId, String commandStoreId, String accountId, String appId);

  Template getAndSaveImportedTemplate(
      String version, String commandName, String commandStoreName, String accountId, String appId);
}
