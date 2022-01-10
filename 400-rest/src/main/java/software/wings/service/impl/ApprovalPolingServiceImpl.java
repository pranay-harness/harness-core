/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApprovalPolingService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ApprovalPolingServiceImpl implements ApprovalPolingService {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public String save(ApprovalPollingJobEntity approvalPollingJobEntity) {
    try {
      return wingsPersistence.save(approvalPollingJobEntity);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.JIRA_ERROR).addParam("message", e.getMessage());
    }
  }

  @Override
  public void delete(String entityId) {
    wingsPersistence.delete(ApprovalPollingJobEntity.class, entityId);
  }

  @Override
  public void updateNextIteration(String entityId, long nextIteration) {
    wingsPersistence.updateField(ApprovalPollingJobEntity.class, entityId, "nextIteration", nextIteration);
  }
}
