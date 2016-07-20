package software.wings.service.impl;

import static com.google.common.collect.ImmutableMap.*;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ErrorCodes.PLATFORM_SOFTWARE_DELETE_ERROR;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Base;
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
    PageRequest<AppContainer> req =
        aPageRequest()
            .withLimit(request.getLimit())
            .withOffset(request.getOffset())
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, Base.GLOBAL_APP_ID).build())
            .build(); // FIXME
    return wingsPersistence.query(AppContainer.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer get(String appId, String platformId) {
    appId = GLOBAL_APP_ID; // FIXME
    return wingsPersistence.get(AppContainer.class, appId, platformId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer save(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    appContainer.setAppId(GLOBAL_APP_ID); // FIXME
    String fileId = fileService.saveFile(appContainer, in, fileBucket);
    appContainer.setFileUuid(fileId);
    return wingsPersistence.saveAndGet(AppContainer.class, appContainer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer update(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    AppContainer storedAppContainer =
        wingsPersistence.get(AppContainer.class, appContainer.getAppId(), appContainer.getUuid());
    if (newPlatformSoftwareBinaryUploaded(storedAppContainer, appContainer)) {
      String fileId = fileService.saveFile(appContainer, in, fileBucket);
      appContainer.setFileUuid(fileId);
    }
    appContainer.setAppId(storedAppContainer.getAppId());
    appContainer.setUuid(storedAppContainer.getUuid());
    wingsPersistence.updateFields(AppContainer.class, appContainer.getUuid(),
        of("name", appContainer.getName(), "description", appContainer.getDescription(), "version",
            appContainer.getVersion(), "source", appContainer.getSource()));
    return get(appContainer.getAppId(), appContainer.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String appContainerId) {
    appId = GLOBAL_APP_ID; // FIXME
    ensureAppContainerNotInUse(appContainerId);
    // safe to delete
    AppContainer appContainer = wingsPersistence.get(AppContainer.class, appId, appContainerId);
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
    final String globalAppId = GLOBAL_APP_ID; // FIXME
    return (Map<String, String>) list(
        aPageRequest().addFilter(aSearchFilter().withField("appId", Operator.EQ, globalAppId).build()).build())
        .getResponse()
        .stream()
        .collect(toMap(AppContainer::getUuid, AppContainer::getName));
  }
}
