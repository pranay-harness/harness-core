package software.wings.service.intfc;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.service.intfc.ownership.OwnedByService;

public interface ApplicationManifestService extends OwnedByService {
  ApplicationManifest create(ApplicationManifest applicationManifest);

  ApplicationManifest update(ApplicationManifest applicationManifest);

  ApplicationManifest get(String appId, String serviceId);
}