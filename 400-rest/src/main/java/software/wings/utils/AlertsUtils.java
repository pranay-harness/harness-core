/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.yaml.errorhandling.GitSyncError;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AlertsUtils {
  @Inject AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;
  public void closeAlertIfApplicable(String accountId) {
    final long errorCount =
        wingsPersistence.createQuery(GitSyncError.class).filter(GitSyncError.ACCOUNT_ID_KEY2, accountId).count();
    if (errorCount == 0) {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.GitSyncError, getGitSyncErrorAlert(accountId));
    }
  }

  public GitSyncErrorAlert getGitSyncErrorAlert(String accountId) {
    return GitSyncErrorAlert.builder().accountId(accountId).message("Unable to process changes from Git").build();
  }
}
