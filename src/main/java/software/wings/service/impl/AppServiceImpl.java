package software.wings.service.impl;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Metered;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

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
    platformSoftware.setBinaryDocumentId(fileID);
    return wingsPersistence.save(platformSoftware);
  }

  @Override
  public String updatePlatformSoftware(
      String platformID, PlatformSoftware platformSoftware, InputStream in, FileBucket fileBucket) {
    PlatformSoftware storedPlatformSoftware = wingsPersistence.get(PlatformSoftware.class, platformID);
    if (newPlatformSoftwareBinaryUploaded(storedPlatformSoftware, platformSoftware)) {
      String fileID = fileService.saveFile(platformSoftware, in, fileBucket);
      platformSoftware.setBinaryDocumentId(fileID);
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