package software.wings.service.intfc;

import software.wings.beans.AppContainer;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;

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
  String save(AppContainer appContainer, InputStream inputStream, FileBucket bucket);

  /**
   * Update.
   *
   * @param platformId   the platform id
   * @param appContainer the app container
   * @param inputStream  the input stream
   * @param bucket       the bucket
   * @return the string
   */
  String update(String platformId, AppContainer appContainer, InputStream inputStream, FileBucket bucket);

  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<AppContainer> list(PageRequest<AppContainer> request);

  /**
   * Gets the.
   *
   * @param appId          the app id
   * @param appContainerId the app container id
   * @return the app container
   */
  AppContainer get(String appId, String appContainerId);

  /**
   * Delete.
   *
   * @param appId          the app id
   * @param appContainerId the app container id
   */
  void delete(String appId, String appContainerId);

  /**
   * Delete by app id.
   *
   * @param appId the app id
   */
  void deleteByAppId(String appId);
}
