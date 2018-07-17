package software.wings.service.intfc.template;

import static software.wings.beans.template.TemplateVersion.ChangeType;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateVersion;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

public interface TemplateVersionService {
  PageResponse<TemplateVersion> listTemplateVersions(PageRequest<TemplateVersion> pageRequest);

  TemplateVersion lastTemplateVersion(@NotEmpty String accountId, @NotEmpty String templateUuid);

  TemplateVersion newTemplateVersion(@NotEmpty String accountId, @NotEmpty String galleryId,
      @NotEmpty String templateUuid, @NotEmpty String templateType, @NotEmpty String templateName,
      @NotEmpty ChangeType changeType);
}
