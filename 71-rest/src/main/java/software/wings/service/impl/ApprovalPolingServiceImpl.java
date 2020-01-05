package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApprovalPolingService;

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
}
