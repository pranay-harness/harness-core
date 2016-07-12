package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;
import static software.wings.beans.ErrorCodes.PLATFORM_SOFTWARE_DELETE_ERROR;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.stencils.DataProvider;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/4/16.
 */
@ValidateOnExecution
@Singleton
public class AppContainerServiceImpl implements AppContainerService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<AppContainer> list(PageRequest<AppContainer> request) {
    return wingsPersistence.query(AppContainer.class, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer get(String appId, String platformId) {
    return wingsPersistence.get(AppContainer.class, appId, platformId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String save(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    String fileId = fileService.saveFile(appContainer, in, fileBucket);
    appContainer.setFileUuid(fileId);
    return wingsPersistence.save(appContainer);
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String appContainerId) {
    ensureAppContainerNotInUse(appContainerId);
    // safe to delete
    AppContainer appContainer = wingsPersistence.get(AppContainer.class, appContainerId);
    wingsPersistence.delete(AppContainer.class, appContainerId);
    fileService.deleteFile(appContainer.getFileUuid(), PLATFORMS);
  }

  private void ensureAppContainerNotInUse(String appContainerId) {
    Application application = wingsPersistence.createQuery(Application.class).retrievedFields(true, "services").get();
    if (application != null && application.getServices() != null) {
      for (Service service : application.getServices()) {
        if (service.getAppContainer().getUuid().equals(appContainerId)) {
          throw new WingsException(PLATFORM_SOFTWARE_DELETE_ERROR);
        }
      }
    }
  }

  @Override
  public void deleteByAppId(String appId) {
    List<AppContainer> containers =
        wingsPersistence.createQuery(AppContainer.class).field("appId").equal(appId).asList();
    if (containers != null) {
      containers.forEach(appContainer -> delete(appId, appContainer.getUuid()));
    }
  }

  private boolean newPlatformSoftwareBinaryUploaded(AppContainer storedAppContainer, AppContainer appContainer) {
    if (storedAppContainer.getSource().equals(appContainer.getSource())) {
      if (appContainer.getChecksum() != null && appContainer.getChecksum().equals(storedAppContainer.getChecksum())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    return (Map<String, String>) list(
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build()).build())
        .getResponse()
        .stream()
        .collect(toMap(AppContainer::getUuid, AppContainer::getName));
  }
}
