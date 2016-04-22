package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorConstants.PLATFORM_SOFTWARE_DELETE_ERROR;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
public class AppServiceImpl implements AppService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  @Metered
  public Application save(Application app) {
    return wingsPersistence.saveAndGet(Application.class, app);
  }

  @Override
  public List<Application> list() {
    return wingsPersistence.list(Application.class);
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  @Override
  public Application findByUuid(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    wingsPersistence.update(query, operations);
    return wingsPersistence.get(Application.class, app.getUuid());
  }

  @Override
  public String savePlatformSoftware(PlatformSoftware platformSoftware, InputStream in, FileBucket fileBucket) {
    String fileId = fileService.saveFile(platformSoftware, in, fileBucket);
    platformSoftware.setFileUUID(fileId);
    return wingsPersistence.save(platformSoftware);
  }

  @Override
  public String updatePlatformSoftware(
      String platformId, PlatformSoftware platformSoftware, InputStream in, FileBucket fileBucket) {
    PlatformSoftware storedPlatformSoftware = wingsPersistence.get(PlatformSoftware.class, platformId);
    if (newPlatformSoftwareBinaryUploaded(storedPlatformSoftware, platformSoftware)) {
      String fileId = fileService.saveFile(platformSoftware, in, fileBucket);
      platformSoftware.setFileUUID(fileId);
    }
    platformSoftware.setAppID(storedPlatformSoftware.getAppID());
    platformSoftware.setUuid(storedPlatformSoftware.getUuid());
    return wingsPersistence.save(platformSoftware);
  }

  @Override
  public List<PlatformSoftware> getPlatforms(String appId) {
    Query<PlatformSoftware> query = wingsPersistence.createQuery(PlatformSoftware.class).field("appID").equal(appId);
    return query.asList();
  }

  @Override
  public PlatformSoftware getPlatform(String appId, String platformId) {
    return wingsPersistence.get(PlatformSoftware.class, platformId);
  }

  @Override
  public void deletePlatform(String appId, String platformId) {
    Application application = wingsPersistence.createQuery(Application.class).retrievedFields(true, "services").get();
    for (Service service : application.getServices()) {
      for (PlatformSoftware platformSoftware : service.getPlatformSoftwares()) {
        if (platformSoftware.getUuid().equals(platformId)) {
          throw new WingsException(PLATFORM_SOFTWARE_DELETE_ERROR);
        }
      }
    }
    // safe to delete
    PlatformSoftware platformSoftware = wingsPersistence.get(PlatformSoftware.class, platformId);
    wingsPersistence.delete(PlatformSoftware.class, platformId);
    fileService.deleteFile(platformSoftware.getFileUUID(), PLATFORMS);
  }

  @Override
  public void deleteApp(String appId) {
    wingsPersistence.delete(Application.class, appId);
  }

  private boolean newPlatformSoftwareBinaryUploaded(
      PlatformSoftware storedPlatformSoftware, PlatformSoftware platformSoftware) {
    if (storedPlatformSoftware.getSource().equals(platformSoftware.getSource())) {
      if (platformSoftware.getChecksum() != null
          && platformSoftware.getChecksum().equals(storedPlatformSoftware.getChecksum())) {
        return false;
      }
    }
    return true;
  }
}
