package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.AppContainer;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.io.InputStream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/4/16.
 */
public interface AppContainerService {
  /**
   * Save.
   *
   * @param appContainer the app container
   * @param inputStream  the input stream
   * @param bucket       the bucket
   * @return the string
   */
  @ValidationGroups(Create.class)
  AppContainer save(@Valid AppContainer appContainer, @NotNull InputStream inputStream, @NotNull FileBucket bucket);

  /**
   * Save app container referecnce to file uuid
   * @param appContainer
   * @return
   */
  AppContainer save(@Valid AppContainer appContainer);
  /**
   * Update.
   *
   * @param appContainer the app container
   * @param inputStream  the input stream
   * @param bucket       the bucket
   * @return the string
   */
  @ValidationGroups(Update.class)
  AppContainer update(@Valid AppContainer appContainer, @NotNull InputStream inputStream, @NotNull FileBucket bucket);

  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<AppContainer> list(@NotNull PageRequest<AppContainer> request);

  /**
   * Gets the.
   *
   *
   * @param accountId
   * @param appContainerId the app container id
   * @return the app container
   */
  AppContainer get(@NotEmpty String accountId, @NotEmpty String appContainerId);

  /**
   * Delete.
   *
   * @param accountId
   * @param appContainerId the app container id
   */
  void delete(String accountId, @NotEmpty String appContainerId);

  /**
   * Download file.
   *
   * @param accountId      the account id
   * @param appContainerId the app container id
   * @return the file
   */
  File download(String accountId, String appContainerId);
}
