package software.wings.service.intfc.instana;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.InstanaConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.instana.InstanaAnalyzeMetricRequest;
import software.wings.service.impl.instana.InstanaAnalyzeMetrics;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;

import java.util.List;

public interface InstanaDelegateService {
  @DelegateTaskType(TaskType.INSTANA_GET_INFRA_METRICS)
  InstanaInfraMetrics getInfraMetrics(InstanaConfig instanaConfig, List<EncryptedDataDetail> encryptionDetails,
      InstanaInfraMetricRequest infraMetricRequest, ThirdPartyApiCallLog apiCallLog) throws DataCollectionException;

  @DelegateTaskType(TaskType.INSTANA_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(InstanaConfig instanaConfig, List<EncryptedDataDetail> encryptedDataDetails);

  InstanaAnalyzeMetrics getInstanaTraceMetrics(InstanaConfig instanaConfig,
      List<EncryptedDataDetail> encryptedDataDetails, InstanaAnalyzeMetricRequest instanaAnalyzeMetricRequest,
      ThirdPartyApiCallLog thirdPartyApiCallLog);
}
