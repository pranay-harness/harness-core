package software.wings.service.intfc.template;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.template.TemplateFolder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

public interface TemplateFolderService {
  PageResponse<TemplateFolder> list(PageRequest<TemplateFolder> pageRequest);

  @ValidationGroups(Create.class) TemplateFolder save(@Valid TemplateFolder templateFolder);

  @ValidationGroups(Update.class) TemplateFolder update(@Valid TemplateFolder templateFolder);

  boolean delete(String templateFolderUuid);

  TemplateFolder get(@NotEmpty String uuid);

  void loadDefaultTemplateFolders();

  TemplateFolder getTemplateTree(@NotEmpty String accountId, String keyword, List<String> templateTypes);

  void copyHarnessTemplateFolders(@NotEmpty String galleryId, @NotEmpty String accountId, @NotEmpty String accountName);

  TemplateFolder getByFolderPath(@NotEmpty String accountId, @NotEmpty String folderPath);

  Map<String, String> fetchTemplateFolderNames(@NotEmpty String accountId, List<String> folderUuids);
}
