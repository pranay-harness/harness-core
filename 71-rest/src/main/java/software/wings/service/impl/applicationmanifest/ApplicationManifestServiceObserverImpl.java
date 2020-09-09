package software.wings.service.impl.applicationmanifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.service.intfc.applicationmanifest.ApplicationManifestServiceObserver;

@OwnedBy(HarnessTeam.CDC)
public class ApplicationManifestServiceObserverImpl implements ApplicationManifestServiceObserver {
  @Override
  public void onSaved(ApplicationManifest applicationManifest) {
    // TODO
    throw new UnsupportedOperationException("Work in Progress");
  }

  @Override
  public void onUpdated(ApplicationManifest applicationManifest) {
    // TODO
    throw new UnsupportedOperationException("Work in Progress");
  }

  @Override
  public void onDeleted(ApplicationManifest applicationManifest) {
    // TODO
    throw new UnsupportedOperationException("Work in Progress");
  }
}
