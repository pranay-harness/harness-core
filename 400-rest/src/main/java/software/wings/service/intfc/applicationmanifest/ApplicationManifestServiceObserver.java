/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
