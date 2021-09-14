/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static software.wings.beans.Application.GLOBAL_APP_ID;

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
