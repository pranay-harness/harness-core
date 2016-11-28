package software.wings.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.common.io.Files;

import software.wings.beans.AppContainer;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.DataProvider;
import software.wings.utils.FileType;
import software.wings.utils.FileTypeDetector;
import software.wings.utils.Misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/4/16.
 */
@ValidateOnExecution
@Singleton
public class AppContainerServiceImpl implements AppContainerService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private ServiceResourceService serviceResourceService;

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
  public AppContainer save(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    appContainer.setAppId(GLOBAL_APP_ID); // FIXME
    uploadAppContainerFile(appContainer, in, fileBucket);

    return wingsPersistence.saveAndGet(AppContainer.class, appContainer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer update(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    AppContainer storedAppContainer = wingsPersistence.get(AppContainer.class, GLOBAL_APP_ID, appContainer.getUuid());
    if (newPlatformSoftwareBinaryUploaded(storedAppContainer, appContainer)) {
      uploadAppContainerFile(appContainer, in, fileBucket);
    }
    appContainer.setAppId(storedAppContainer.getAppId());
    appContainer.setUuid(storedAppContainer.getUuid());
    wingsPersistence.updateFields(AppContainer.class, appContainer.getUuid(),
        of("name", appContainer.getName(), "description", appContainer.getDescription(), "source",
            appContainer.getSource()));
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

  private void ensureAppContainerNotInUse(String appContainerId) {
    List<Service> services =
        serviceResourceService.list(aPageRequest().addFilter("appContainer", Operator.EQ, appContainerId).build())
            .getResponse();
    if (services.size() > 0) {
      throw new WingsException(INVALID_REQUEST, "message",
          String.format(
              "Application stack is in use by %s service%s.", services.size(), services.size() == 1 ? "" : "s"));
    }
  }

  private void uploadAppContainerFile(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    String fileId = fileService.saveFile(appContainer, in, fileBucket);
    appContainer.setFileUuid(fileId);

    File tempFile = new File(System.getProperty("java.io.tmpdir"), "appStack" + Thread.currentThread().getId());
    fileService.download(fileId, tempFile, fileBucket);

    Misc.ignoreException(() -> {
      BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(tempFile));
      FileType fileType = FileTypeDetector.detectType(bufferedInputStream);
      appContainer.setFileType(fileType);
      appContainer.setStackRootDirectory(fileType.getRoot(bufferedInputStream));
    });
  }

  @Override
  public File download(String accountId, String appContainerId) {
    AppContainer appContainer = get(GLOBAL_APP_ID, appContainerId);
    File file = new File(Files.createTempDir(), appContainer.getFileName());
    fileService.download(appContainer.getFileUuid(), file, PLATFORMS);
    return file;
  }
}
