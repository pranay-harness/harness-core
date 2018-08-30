package software.wings.service.intfc.sumo;

import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Delegate Service for SumoLogic.
 *
 * Created by sriram_parthasarathy on 9/11/17.
 */
public interface SumoDelegateService {
  /**
   * Method to validate sumo Logic configuration.
   * @param sumoConfig
   * @param encryptedDataDetails
   * @return
   */
  @DelegateTaskType(TaskType.SUMO_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(@NotNull SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Method to fetch sample log data for given config.
   * @param value
   * @param index
   * @param encryptedDataDetails
   * @return
   */
  @DelegateTaskType(TaskType.SUMO_GET_HOST_RECORDS)
  Object getLogSample(SumoConfig value, String index, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Method to fetch log test data by host.
   * @param accountId
   * @param config
   * @param query
   * @param hostNameField
   * @param hostName
   * @param encryptedDataDetails
   * @return
   */
  @DelegateTaskType(TaskType.SUMO_GET_LOG_DATA_BY_HOST)
  VerificationNodeDataSetupResponse getLogDataByHost(String accountId, SumoConfig config, String query,
      String hostNameField, String hostName, List<EncryptedDataDetail> encryptedDataDetails);
}
