package software.wings.service.intfc.applicationmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.appmanifest.ApplicationManifest;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ApplicationManifestServiceObserver {
  void onSaved(@NotNull ApplicationManifest applicationManifest);
  void onUpdated(@NotNull ApplicationManifest applicationManifest);
  void onDeleted(@NotNull ApplicationManifest applicationManifest);
}
