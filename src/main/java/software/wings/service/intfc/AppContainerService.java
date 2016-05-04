package software.wings.service.intfc;

import software.wings.beans.AppContainer;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;

/**
 * Created by anubhaw on 5/4/16.
 */
public interface AppContainerService {
  String save(AppContainer appContainer, InputStream inputStream, FileBucket bucket);

  String update(String platformId, AppContainer appContainer, InputStream inputStream, FileBucket bucket);

  PageResponse<AppContainer> list(PageRequest<AppContainer> request);

  AppContainer get(String appContainerId);

  void delete(String appContainerId);
}
