package software.wings.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.mongodb.morphia.mapping.Mapper;
import software.wings.beans.AppContainer;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.FileType;
import software.wings.utils.FileTypeDetector;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

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
public class AppContainerServiceImpl implements AppContainerService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;

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
  public AppContainer get(String accountId, String platformId) {
    return wingsPersistence.createQuery(AppContainer.class)
        .field("accountId")
        .equal(accountId)
        .field(Mapper.ID_KEY)
        .equal(platformId)
        .get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer save(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    uploadAppContainerFile(appContainer, in, fileBucket);
    return wingsPersistence.saveAndGet(AppContainer.class, appContainer);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer save(AppContainer appContainer) {
    return wingsPersistence.saveAndGet(AppContainer.class, appContainer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AppContainer update(AppContainer appContainer, InputStream in, FileBucket fileBucket) {
    AppContainer storedAppContainer = get(appContainer.getAccountId(), appContainer.getUuid());
    Validator.notNullCheck("App Stack", storedAppContainer);
    if (storedAppContainer.isSystemCreated()) {
      throw new WingsException(INVALID_REQUEST, "message", "System created Application Stack can not be updated.");
    }
    if (newPlatformSoftwareBinaryUploaded(storedAppContainer, appContainer)) {
      uploadAppContainerFile(appContainer, in, fileBucket);
    }
    wingsPersistence.updateFields(AppContainer.class, appContainer.getUuid(),
        of("name", appContainer.getName(), "description", appContainer.getDescription(), "version",
            appContainer.getVersion(), "hardened", appContainer.isHardened()));
    return get(appContainer.getAccountId(), appContainer.getUuid());
  }

  @Override
  public AppContainer update(AppContainer appContainer) {
    AppContainer storedAppContainer = get(appContainer.getAccountId(), appContainer.getUuid());
    Validator.notNullCheck("App Stack", storedAppContainer);
    Map<String, Object> updatedFields = Maps.newHashMap();
    if (appContainer.getStackRootDirectory() != null) {
      updatedFields.put("stackRootDirectory", appContainer.getStackRootDirectory());
    }
    updatedFields.put("name", appContainer.getName());
    updatedFields.put("description", appContainer.getDescription());
    updatedFields.put("version", appContainer.getVersion());
    updatedFields.put("hardened", appContainer.isHardened());
    updatedFields.put("fileName", appContainer.getFileName());
    wingsPersistence.updateFields(AppContainer.class, appContainer.getUuid(), updatedFields);
    return get(appContainer.getAccountId(), appContainer.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String accountId, String appContainerId) {
    AppContainer appContainer = get(accountId, appContainerId);
    Validator.notNullCheck("App Stack", appContainer);
    ensureAppContainerNotInUse(appContainerId);
    // safe to delete
    wingsPersistence.delete(AppContainer.class, appContainerId);
    if (!appContainer.isSystemCreated()) {
      fileService.deleteFile(appContainer.getFileUuid(), PLATFORMS);
    }
  }

  private boolean newPlatformSoftwareBinaryUploaded(AppContainer storedAppContainer, AppContainer appContainer) {
    return !(appContainer.getChecksum() != null && appContainer.getChecksum().equals(storedAppContainer.getChecksum()));
  }

  private void ensureAppContainerNotInUse(String appContainerId) {
    List<Service> services =
        serviceResourceService
            .list(aPageRequest().addFilter("appContainer", Operator.EQ, appContainerId).build(), false, true)
            .getResponse();
    if (services.size() > 0) {
      throw new WingsException(INVALID_REQUEST, "message",
          String.format(
              "Application Stack is in use by %s service%s.", services.size(), services.size() == 1 ? "" : "s"));
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
    AppContainer appContainer = get(accountId, appContainerId);
    File file = new File(Files.createTempDir(), appContainer.getFileName());
    fileService.download(appContainer.getFileUuid(), file, PLATFORMS);
    return file;
  }
}
