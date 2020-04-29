package software.wings.helpers.ext.openshift;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.sm.ExecutionContext;
import software.wings.utils.ApplicationManifestUtils;

import javax.annotation.Nonnull;

@Singleton
@Slf4j
public class OpenShiftManagerService {
  @Inject private ApplicationManifestUtils applicationManifestUtils;

  public boolean isOpenShiftManifestConfig(@Nonnull ExecutionContext context) {
    ApplicationManifest applicationManifestForService =
        applicationManifestUtils.getApplicationManifestForService(context);

    if (applicationManifestForService == null) {
      throw new InvalidRequestException("Manifest Config at Service can't be  empty", WingsException.USER);
    }

    return applicationManifestForService.getStoreType() == StoreType.OC_TEMPLATES;
  }
}
