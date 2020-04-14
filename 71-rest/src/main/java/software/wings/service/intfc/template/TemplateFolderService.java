package software.wings.service.intfc.template;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.TemplateFolder;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface TemplateFolderService {
  //  @Deprecated should not be used in future code,
  PageResponse<TemplateFolder> list(PageRequest<TemplateFolder> pageRequest);

  TemplateFolder save(@Valid TemplateFolder templateFolder, String galleryId);

  TemplateFolder saveSafelyAndGet(@Valid TemplateFolder templateFolder, String galleryId);

  TemplateFolder update(@Valid TemplateFolder templateFolder);

  boolean delete(String templateFolderUuid);

  TemplateFolder get(@NotEmpty String uuid);

  TemplateFolder getRootLevelFolder(@NotEmpty String accountId, @NotEmpty String galleryId);

  void loadDefaultTemplateFolders();

  TemplateFolder getTemplateTree(
      @NotEmpty String accountId, String keyword, List<String> templateTypes, String galleryId);

  TemplateFolder getTemplateTree(
      @NotEmpty String accountId, @NotEmpty String appId, String keyword, List<String> templateTypes, String galleryId);

  void copyHarnessTemplateFolders(@NotEmpty String galleryId, @NotEmpty String accountId, @NotEmpty String accountName);

  TemplateFolder getByFolderPath(@NotEmpty String accountId, @NotEmpty String folderPath, String galleryId);

  TemplateFolder getByFolderPath(
      @NotEmpty String accountId, @NotEmpty String appId, @NotEmpty String folderPath, String galleryId);

  Map<String, String> fetchTemplateFolderNames(@NotEmpty String accountId, List<String> folderUuids, String galleryId);

  TemplateFolder createRootImportedTemplateFolder(String accountId, String galleryId);

  TemplateFolder getImportedTemplateFolder(String accountId, String galleryId, String appId);
}
