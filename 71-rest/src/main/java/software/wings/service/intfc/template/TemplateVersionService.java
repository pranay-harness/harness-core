package software.wings.service.intfc.template;

import static software.wings.beans.template.TemplateVersion.ChangeType;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.ImportedCommand;

import java.util.List;

public interface TemplateVersionService {
  PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest);

  ImportedCommand listImportedTemplateVersions(String commandName, String commandStoreName, String accountId);

  List<ImportedCommand> listLatestVersionOfImportedTemplates(
      List<String> commandNames, String commandStoreName, String accountId);

  TemplateVersion lastTemplateVersion(@NotEmpty String accountId, @NotEmpty String templateUuid);

  TemplateVersion newImportedTemplateVersion(String accountId, String galleryId, String templateUuid,
      String templateType, String templateName, String commandVersion, String versionDetails);

  TemplateVersion newTemplateVersion(@NotEmpty String accountId, @NotEmpty String galleryId,
      @NotEmpty String templateUuid, @NotEmpty String templateType, @NotEmpty String templateName,
      @NotEmpty ChangeType changeType);
}
