package software.wings.delegatetasks.manifest;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.appmanifest.ApplicationManifest;

public class ApplicationManifestLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(ApplicationManifest.class);
  public static final String SERVICE_ID = "serviceId";

  public ApplicationManifestLogContext(String appManifestId, String serviceId, OverrideBehavior behavior) {
    super(ImmutableMap.of(ID, appManifestId, SERVICE_ID, serviceId), behavior);
  }

  public ApplicationManifestLogContext(String appManifestId, OverrideBehavior behavior) {
    super(ImmutableMap.of(ID, appManifestId), behavior);
  }
}
