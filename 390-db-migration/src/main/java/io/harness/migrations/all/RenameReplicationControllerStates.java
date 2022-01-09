/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;

import io.harness.migrations.Migration;
import io.harness.migrations.MigrationUtils;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RenameReplicationControllerStates implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Renaming Kubernetes state types and classes");

    MigrationUtils.renameStateTypeAndStateClass(KUBERNETES_SETUP, KUBERNETES_SETUP, wingsPersistence, workflowService);
    MigrationUtils.renameStateTypeAndStateClass(
        KUBERNETES_DEPLOY, KUBERNETES_DEPLOY, wingsPersistence, workflowService);
    MigrationUtils.renameStateTypeAndStateClass(
        KUBERNETES_DEPLOY_ROLLBACK, KUBERNETES_DEPLOY_ROLLBACK, wingsPersistence, workflowService);

    log.info("Finished renaming Kubernetes state types and classes");
  }
}
