package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.AppContainer;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

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
   * @param appId          the app id
   * @param appContainerId the app container id
   * @return the app container
   */
  AppContainer get(@NotEmpty String appId, @NotEmpty String appContainerId);

  /**
   * Delete.
   *
   * @param appId          the app id
   * @param appContainerId the app container id
   */
  void delete(@NotEmpty String appId, @NotEmpty String appContainerId);

  /**
   * Delete by app id.
   *
   * @param appId the app id
   */
  void deleteByAppId(@NotEmpty String appId);
}
