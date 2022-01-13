/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.template;

import static software.wings.beans.template.TemplateVersion.ChangeType;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.ImportedCommand;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

public interface TemplateVersionService {
  PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest);

  ImportedCommand listImportedTemplateVersions(
      String commandName, String commandStoreName, String accountId, String appId);

  List<ImportedCommand> listLatestVersionOfImportedTemplates(
      List<String> commandNames, String commandStoreName, String accountId, String appId);

  TemplateVersion lastTemplateVersion(@NotEmpty String accountId, @NotEmpty String templateUuid);

  TemplateVersion newImportedTemplateVersion(String accountId, String galleryId, String templateUuid,
      String templateType, String templateName, String commandVersion, String versionDetails);

  TemplateVersion newTemplateVersion(@NotEmpty String accountId, @NotEmpty String galleryId,
      @NotEmpty String templateUuid, @NotEmpty String templateType, @NotEmpty String templateName,
      @NotEmpty ChangeType changeType);
}
