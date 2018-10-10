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
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlPushService;

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

  private ApplicationManifest upsertApplicationManifest(ApplicationManifest applicationManifest, boolean isCreate) {
    if (!serviceResourceService.exist(applicationManifest.getAppId(), applicationManifest.getServiceId())) {
      throw new InvalidRequestException("Service doesn't exist");
    }

    if (applicationManifest.getStoreType() == StoreType.Remote) {
      applicationManifest.setManifestFiles(null);
    }

    ApplicationManifest savedApplicationManifest =
        wingsPersistence.saveAndGet(ApplicationManifest.class, applicationManifest);

    String appId = savedApplicationManifest.getAppId();
    String accountId = appService.getAccountIdByAppId(appId);
    Service service = serviceResourceService.get(appId, savedApplicationManifest.getServiceId());

    Type type = isCreate ? Type.CREATE : Type.UPDATE;
    yamlPushService.pushYamlChangeSet(
        accountId, service, savedApplicationManifest, type, savedApplicationManifest.isSyncFromGit());

    return savedApplicationManifest;
  }

  @Override
  public void pruneByService(String appId, String serviceId) {
    Query<ApplicationManifest> query = wingsPersistence.createQuery(ApplicationManifest.class)
                                           .filter(ApplicationManifest.APP_ID_KEY, appId)
                                           .filter(ApplicationManifest.SERVICE_ID_KEY, serviceId);
    wingsPersistence.delete(query);
  }
}
