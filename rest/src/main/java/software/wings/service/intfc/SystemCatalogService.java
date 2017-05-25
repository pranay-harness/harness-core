package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SystemCatalog;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 5/23/17.
 */
public interface SystemCatalogService {
  /**
   * Save
   * @param systemCatalog
   * @param url
   * @param bucket
   * @param size
   * @return
   */
  @ValidationGroups(Create.class)
  SystemCatalog save(@Valid SystemCatalog systemCatalog, @NotNull String url, @NotNull FileService.FileBucket bucket,
      @NotNull long size);

  /**
   * List.
   *
   * @param request the request
   * @return the list of System Catalogs
   */
  List<SystemCatalog> list(@NotNull PageRequest<SystemCatalog> request);
}
