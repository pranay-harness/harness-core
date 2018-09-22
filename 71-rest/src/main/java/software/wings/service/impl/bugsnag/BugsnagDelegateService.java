package software.wings.service.impl.bugsnag;

import software.wings.beans.BugsnagConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.List;
import java.util.Set;

public interface BugsnagDelegateService {
  @DelegateTaskType(TaskType.BUGSNAG_GET_APP_TASK)
  Set<BugsnagApplication> getOrganizations(
      BugsnagConfig config, List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog);
  @DelegateTaskType(TaskType.BUGSNAG_GET_APP_TASK)
  Set<BugsnagApplication> getProjects(BugsnagConfig config, String orgId,
      List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog);
}
