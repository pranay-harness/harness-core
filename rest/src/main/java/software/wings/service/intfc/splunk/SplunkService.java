package software.wings.service.intfc.splunk;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.splunk.SplunkLogDataRecord;
import software.wings.service.impl.splunk.SplunkLogElement;
import software.wings.service.impl.splunk.SplunkLogMLAnalysisRecord;
import software.wings.service.impl.splunk.SplunkLogRequest;
import software.wings.service.impl.splunk.SplunkMLAnalysisSummary;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface SplunkService {
  @ValidationGroups(Create.class)
  Boolean saveLogData(@NotNull String appId, @NotNull String stateExecutionId, String workflowExecutionId,
      @Valid List<SplunkLogElement> logData) throws IOException;

  @ValidationGroups(Create.class) List<SplunkLogDataRecord> getSplunkLogData(@Valid SplunkLogRequest logRequest);

  Boolean markProcessed(@NotNull String stateExecutionId, @NotNull String applicationId, long timeStamp);

  Boolean saveSplunkAnalysisRecords(SplunkLogMLAnalysisRecord mlAnalysisResponse);

  SplunkLogMLAnalysisRecord getSplunkAnalysisRecords(String applicationId, String stateExecutionId, String query);

  boolean isLogDataCollected(String applicationId, String stateExecutionId, String query, int logCollectionMinute);

  SplunkMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId);

  void validateConfig(@NotNull SettingAttribute settingAttribute);
}
