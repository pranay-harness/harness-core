package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateFolder;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface TemplateFolderService {
  PageResponse<TemplateFolder> list(PageRequest<TemplateFolder> pageRequest);

  TemplateFolder save(@Valid TemplateFolder templateFolder);

  TemplateFolder saveSafelyAndGet(@Valid TemplateFolder templateFolder);

  TemplateFolder update(@Valid TemplateFolder templateFolder);

  boolean delete(String templateFolderUuid);

  TemplateFolder get(@NotEmpty String uuid);

  TemplateFolder getRootLevelFolder(@NotEmpty String accountId, @NotEmpty String galleryId);

  void loadDefaultTemplateFolders();

  TemplateFolder getTemplateTree(@NotEmpty String accountId, String keyword, List<String> templateTypes);

  TemplateFolder getTemplateTree(
      @NotEmpty String accountId, @NotEmpty String appId, String keyword, List<String> templateTypes);

  void copyHarnessTemplateFolders(@NotEmpty String galleryId, @NotEmpty String accountId, @NotEmpty String accountName);

  TemplateFolder getByFolderPath(@NotEmpty String accountId, @NotEmpty String folderPath);

  TemplateFolder getByFolderPath(@NotEmpty String accountId, @NotEmpty String appId, @NotEmpty String folderPath);

  Map<String, String> fetchTemplateFolderNames(@NotEmpty String accountId, List<String> folderUuids);
}
