package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Event.Type;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class ApplicationManifestServiceImpl implements ApplicationManifestService {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationManifestServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private YamlPushService yamlPushService;

  @Override
  public ApplicationManifest create(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, true);
  }

  @Override
  public ManifestFile createManifestFile(ManifestFile manifestFile, String serviceId) {
    return upsertApplicationManifestFile(manifestFile, serviceId, true);
  }

  @Override
  public ManifestFile updateManifestFile(ManifestFile manifestFile, String serviceId) {
    return upsertApplicationManifestFile(manifestFile, serviceId, false);
  }

  @Override
  public ApplicationManifest update(ApplicationManifest applicationManifest) {
    return upsertApplicationManifest(applicationManifest, false);
  }

  @Override
  public ApplicationManifest get(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId);
    return query.get();
  }

  @Override
  public ApplicationManifest getById(String appId, String id) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.ID_KEY, id)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId);
    return query.get();
  }

  @Override
  public List<ManifestFile> getManifestFiles(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId);
    ApplicationManifest applicationManifest = query.get();

    return getManifestFilesByAppManifestId(appId, applicationManifest.getUuid());
  }

  @Override
  public List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId) {
    return wingsPersistence.createQuery(ManifestFile.class)
        .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
        .filter(ManifestFile.APP_ID_KEY, appId)
        .asList();
  }

  @Override
  public ManifestFile getManifestFileById(String appId, String id) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ApplicationManifest.APP_ID_KEY, appId)
                                    .filter(ApplicationManifest.ID_KEY, id);
    return query.get();
  }

  @Override
  public ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
                                    .filter(ManifestFile.APP_MANIFEST_FILE_NAME, fileName);
    return query.get();
  }

  private ApplicationManifest upsertApplicationManifest(ApplicationManifest applicationManifest, boolean isCreate) {
    if (!serviceResourceService.exist(applicationManifest.getAppId(), applicationManifest.getServiceId())) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    ApplicationManifest savedApplicationManifest =
        wingsPersistence.saveAndGet(ApplicationManifest.class, applicationManifest);

    if (StoreType.Remote.equals(savedApplicationManifest.getStoreType())) {
      deleteManifestFiles(applicationManifest.getAppId(), applicationManifest.getUuid());
    }

    String appId = savedApplicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, savedApplicationManifest.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, savedApplicationManifest, type, savedApplicationManifest.isSyncFromGit());

    return savedApplicationManifest;
  }

  public ManifestFile upsertApplicationManifestFile(ManifestFile manifestFile, String serviceId, boolean isCreate) {
    if (!serviceResourceService.exist(manifestFile.getAppId(), serviceId)) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    ApplicationManifest applicationManifest = get(manifestFile.getAppId(), serviceId);
    if (applicationManifest == null) {
      throw new InvalidRequestException("Application Manifest doesn't exist for Service: " + serviceId);
    }

    manifestFile.setApplicationManifestId(applicationManifest.getUuid());
    ManifestFile savedManifestFile = wingsPersistence.saveAndGet(ManifestFile.class, manifestFile);

    String appId = applicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, applicationManifest.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(accountId, service, savedManifestFile, type, savedManifestFile.isSyncFromGit());

    return savedManifestFile;
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    ApplicationManifest applicationManifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                                  .filter(ApplicationManifest.APP_ID_KEY, appId)
                                                  .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId)
                                                  .get();

    if (applicationManifest != null) {
      deleteManifestFiles(appId, applicationManifest.getUuid());

      Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                             .filter(ApplicationManifest.APP_ID_KEY, appId)
                                             .filter(ApplicationManifest.ID_KEY, applicationManifest.getUuid());
      wingsPersistence.delete(query);
    }
  }

  @Override
  public void deleteManifestFiles(String appId, String applicationManifestId) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_MANIFEST_ID_KEY, applicationManifestId)
                                    .filter(ManifestFile.APP_ID_KEY, appId);
    wingsPersistence.delete(query);
  }

  @Override
  public void deleteManifestFileById(String appId, String manifestFileId) {
    Query<ManifestFile> query = wingsPersistence.createQuery(ManifestFile.class)
                                    .filter(ManifestFile.APP_ID_KEY, appId)
                                    .filter(ManifestFile.ID_KEY, manifestFileId);
    wingsPersistence.delete(query);
  }
}
