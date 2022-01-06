/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.applicationmanifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.applicationmanifest.ApplicationManifestServiceObserver;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
public class ManifestPerpetualTaskManger implements ApplicationManifestServiceObserver {
  @Inject private AppManifestPTaskHelper appManifestPTaskHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;

  @Override
  public void onSaved(ApplicationManifest applicationManifest) {
    if (isPollingEnabled(applicationManifest)) {
      appManifestPTaskHelper.createPerpetualTask(applicationManifest);
    }
  }

  @Override
  public void onUpdated(ApplicationManifest applicationManifest) {
    if (isPollingEnabled(applicationManifest)) {
      if (applicationManifest.getPerpetualTaskId() == null) {
        appManifestPTaskHelper.createPerpetualTask(applicationManifest);
      } else {
        appManifestPTaskHelper.resetPerpetualTask(applicationManifest);
      }
    }
  }

  @Override
  public void onDeleted(ApplicationManifest applicationManifest) {
    if (applicationManifest.getPerpetualTaskId() != null) {
      appManifestPTaskHelper.deletePerpetualTask(
          applicationManifest.getPerpetualTaskId(), applicationManifest.getUuid(), applicationManifest.getAccountId());
    }
  }

  private boolean isPollingEnabled(ApplicationManifest applicationManifest) {
    if (applicationManifest.getPollForChanges() == null) {
      return false;
    }
    if (applicationManifest.getAccountId() == null) {
      applicationManifest.setAccountId(appService.getAccountIdByAppId(applicationManifest.getAppId()));
    }
    return featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, applicationManifest.getAccountId())
        && applicationManifest.getPollForChanges();
  }
}
