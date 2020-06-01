package software.wings.service.intfc.splunk;

import com.splunk.Service;
import io.harness.cvng.beans.CVHistogram;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.splunk.SplunkSampleResponse;
import software.wings.service.impl.splunk.SplunkSavedSearch;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Splunk Delegate Service.
 *
 * Created by rsingh on 4/17/17.
 */
public interface SplunkDelegateService {
  /**
   * Method to validate Splunk config.
   * @param splunkConfig
   * @param encryptedDataDetails
   * @return
   */
  @DelegateTaskType(TaskType.SPLUNK_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(@NotNull SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Method to initialize the Splunk Service.
   * @param splunkConfig
   * @param encryptedDataDetails
   * @return
   */
  Service initSplunkService(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Method that fetches log records basd on given config and query.
   * @param splunkConfig
   * @param encryptedDataDetails
   * @param basicQuery
   * @param hostNameField
   * @param host
   * @param startTime
   * @param endTime
   * @param apiCallLog
   * @return
   */
  @DelegateTaskType(TaskType.SPLUNK_GET_HOST_RECORDS)
  List<LogElement> getLogResults(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String basicQuery, String hostNameField, String host, long startTime, long endTime,
      ThirdPartyApiCallLog apiCallLog, int logCollectionMinute, boolean isAdvancedQuery);
  @DelegateTaskType(TaskType.SPLUNK_GET_SAVED_SEARCHES)
  List<SplunkSavedSearch> getSavedSearches(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails);
  @DelegateTaskType(TaskType.SPLUNK_GET_HISTOGRAM)
  CVHistogram getHistogram(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails, String query);
  @DelegateTaskType(TaskType.SPLUNK_GET_SAMPLE)
  SplunkSampleResponse getSamples(
      SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails, String query);
}
