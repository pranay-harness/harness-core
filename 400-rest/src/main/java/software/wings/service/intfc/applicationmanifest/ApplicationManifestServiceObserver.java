/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
