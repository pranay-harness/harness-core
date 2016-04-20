package software.wings.service.impl;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Metered;

import software.wings.beans.*;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorConstants.PLATFORM_SOFTWARE_DELETE_ERROR;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

/**
 *  Application Service Implementation class.
 *
 *
 * @author Rishi
 *
 */
@Singleton
public class AppServiceImpl implements AppService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  @Metered
  public Application save(Application app) {
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    return wingsPersistence.get(Application.class, app.getUuid());
  }

  @Override
  public List<Application> list() {
    return wingsPersistence.list(Application.class);
  }

  @Override
  public PageResponse<Application> list(PageRequest<Application> req) {
    return wingsPersistence.query(Application.class, req);
  }

  private static Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

  @Override
  public Application findByUUID(String uuid) {
    return wingsPersistence.get(Application.class, uuid);
  }

  @Override
  public Application update(Application app) {
    return save(app);
  }

  @Override
  public String savePlatformSoftware(PlatformSoftware platformSoftware, InputStream in, FileBucket fileBucket) {
    String fileID = fileService.saveFile(platformSoftware, in, fileBucket);
    platformSoftware.setFileUUID(fileID);
    return wingsPersistence.save(platformSoftware);
  }

  @Override
  public String updatePlatformSoftware(
      String platformID, PlatformSoftware platformSoftware, InputStream in, FileBucket fileBucket) {
    PlatformSoftware storedPlatformSoftware = wingsPersistence.get(PlatformSoftware.class, platformID);
    if (newPlatformSoftwareBinaryUploaded(storedPlatformSoftware, platformSoftware)) {
      String fileID = fileService.saveFile(platformSoftware, in, fileBucket);
      platformSoftware.setFileUUID(fileID);
    }
    platformSoftware.setAppID(storedPlatformSoftware.getAppID());
    platformSoftware.setUuid(storedPlatformSoftware.getUuid());
    return wingsPersistence.save(platformSoftware);
  }

  @Override
  public List<PlatformSoftware> getPlatforms(String appID) {
    Query<PlatformSoftware> query = wingsPersistence.createQuery(PlatformSoftware.class).field("appID").equal(appID);
    return query.asList();
  }

  @Override
  public PlatformSoftware getPlatform(String appID, String platformID) {
    return wingsPersistence.get(PlatformSoftware.class, platformID);
  }

  @Override
  public void deletePlatform(String appID, String platformID) {
    Application application = wingsPersistence.createQuery(Application.class).retrievedFields(true, "services").get();
    for (Service service : application.getServices()) {
      for (PlatformSoftware platformSoftware : service.getPlatformSoftwares()) {
        if (platformSoftware.getUuid().equals(platformID)) {
          throw new WingsException(PLATFORM_SOFTWARE_DELETE_ERROR);
        }
      }
    }
    // safe to delete
    PlatformSoftware platformSoftware = wingsPersistence.get(PlatformSoftware.class, platformID);
    wingsPersistence.delete(PlatformSoftware.class, platformID);
    fileService.deleteFile(platformSoftware.getFileUUID(), PLATFORMS);
  }

  @Override
  public void deleteApp(String appID) {
    wingsPersistence.delete(Application.class, appID);
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