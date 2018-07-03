package software.wings.service.intfc.elk;

import software.wings.beans.ElkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkLogFetchRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 08/01/17.
 */
public interface ElkDelegateService {
  @DelegateTaskType(TaskType.ELK_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(@NotNull ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails);

  @DelegateTaskType(TaskType.ELK_GET_HOST_RECORDS)
  Object search(@NotNull ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ElkLogFetchRequest logFetchRequest, ThirdPartyApiCallLog apiCallLog) throws IOException;

  @DelegateTaskType(TaskType.ELK_COLLECT_INDICES)
  Map<String, ElkIndexTemplate> getIndices(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException;

  @DelegateTaskType(TaskType.ELK_GET_LOG_SAMPLE)
  Object getLogSample(ElkConfig elkConfig, String index, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException;

  @DelegateTaskType(TaskType.KIBANA_GET_VERSION)
  String getVersion(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException;
}
