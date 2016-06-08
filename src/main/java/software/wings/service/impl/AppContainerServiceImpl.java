package software.wings.service.impl;

import static software.wings.beans.ErrorConstants.PLATFORM_SOFTWARE_DELETE_ERROR;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/4/16.
 */
public class AppContainerServiceImpl implements AppContainerService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  /** {@inheritDoc} */
  @Override
  public PageResponse<AppContainer> list(PageRequest<AppContainer> request) {
    return wingsPersistence.query(AppContainer.class, request);
  }

  /** {@inheritDoc} */
  @Override
  public AppContainer get(String platformId) {
    return wingsPersistence.get(AppContainer.class, platformId);
  }

  /** {@inheritDoc} */
  @Override
  public String save(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    String fileId = fileService.saveFile(appContainer, in, fileBucket);
    appContainer.setFileUuid(fileId);
    return wingsPersistence.save(appContainer);
  }

  /** {@inheritDoc} */
  @Override
  public String update(String platformId, AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    AppContainer storedAppContainer = wingsPersistence.get(AppContainer.class, platformId);
    if (newPlatformSoftwareBinaryUploaded(storedAppContainer, appContainer)) {
      String fileId = fileService.saveFile(appContainer, in, fileBucket);
      appContainer.setFileUuid(fileId);
    }
    appContainer.setAppId(storedAppContainer.getAppId());
    appContainer.setUuid(storedAppContainer.getUuid());
    return wingsPersistence.save(appContainer);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(String appContainerId) {
    Application application = wingsPersistence.createQuery(Application.class).retrievedFields(true, "services").get();
    for (Service service : application.getServices()) {
      if (service.getAppContainer().getUuid().equals(appContainerId)) {
        throw new WingsException(PLATFORM_SOFTWARE_DELETE_ERROR);
      }
    }
    // safe to delete
    AppContainer appContainer = wingsPersistence.get(AppContainer.class, appContainerId);
    wingsPersistence.delete(AppContainer.class, appContainerId);
    fileService.deleteFile(appContainer.getFileUuid(), PLATFORMS);
  }

  private boolean newPlatformSoftwareBinaryUploaded(AppContainer storedAppContainer, AppContainer appContainer) {
    if (storedAppContainer.getSource().equals(appContainer.getSource())) {
      if (appContainer.getChecksum() != null && appContainer.getChecksum().equals(storedAppContainer.getChecksum())) {
        return false;
      }
    }
    return true;
  }
}
