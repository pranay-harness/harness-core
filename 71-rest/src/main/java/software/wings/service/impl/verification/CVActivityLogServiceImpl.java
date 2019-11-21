package software.wings.service.impl.verification;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.CVActivityLogKeys;
import software.wings.verification.CVActivityLog.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CVActivityLogServiceImpl implements CVActivityLogService {
  private static final String EXECUTION_LOGS_NOT_AVAILABLE = "Execution logs are not available for old executions";
  @Inject private WingsPersistence wingsPersistence;
  @Inject(optional = true) private WorkflowService workflowService; // Only available in manager

  @Override
  public List<CVActivityLog> findByCVConfigId(String cvConfigId, long startTimeMilli, long endTimeMilli) {
    PageRequest<CVActivityLog> pageRequest = aPageRequest()
                                                 .withLimit(UNLIMITED)
                                                 .addFilter(CVActivityLogKeys.cvConfigId, Operator.EQ, cvConfigId)
                                                 .addFilter(CVActivityLogKeys.dataCollectionMinute, Operator.GE,
                                                     TimeUnit.MILLISECONDS.toMinutes(startTimeMilli))
                                                 .addFilter(CVActivityLogKeys.dataCollectionMinute, Operator.LT_EQ,
                                                     TimeUnit.MILLISECONDS.toMinutes(endTimeMilli))
                                                 .addOrder(CVActivityLogKeys.createdAt, OrderType.ASC)
                                                 .build();
    return wingsPersistence.query(CVActivityLog.class, pageRequest, excludeAuthority).getResponse();
  }

  @Override
  public Logger getLogger(String cvConfigId, long dataCollectionMinute, String stateExecutionId) {
    return Strings.isNullOrEmpty(cvConfigId) ? getLoggerByStateExecutionId(stateExecutionId)
                                             : getLoggerByCVConfigId(cvConfigId, dataCollectionMinute);
  }

  @Override
  public Logger getLoggerByCVConfigId(String cvConfigId, long dataCollectionMinute) {
    return new LoggerImpl(cvConfigId, dataCollectionMinute);
  }

  @Override
  public Logger getLoggerByStateExecutionId(String stateExecutionId) {
    return new LoggerImpl(stateExecutionId);
  }

  @Override
  public List<CVActivityLog> findByStateExecutionId(String stateExecutionId) {
    PageRequest<CVActivityLog> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(CVActivityLogKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addOrder(CVActivityLogKeys.createdAt, OrderType.ASC)
            .build();
    return wingsPersistence.query(CVActivityLog.class, pageRequest, excludeAuthority).getResponse();
  }

  @Override
  public void saveActivityLogs(List<CVActivityLog> cvActivityLogs) {
    wingsPersistence.save(cvActivityLogs);
  }

  @Override
  public List<CVActivityLog> getActivityLogs(
      String stateExecutionId, String cvConfigId, long startTimeMs, long endTimeMs) {
    List<CVActivityLog> cvActivityLogs;
    if (stateExecutionId != null) {
      AnalysisContext analysisContext = wingsPersistence.createQuery(AnalysisContext.class, excludeAuthority)
                                            .filter(AnalysisContextKeys.stateExecutionId, stateExecutionId)
                                            .get();
      cvActivityLogs = findByStateExecutionId(stateExecutionId);
      if (cvActivityLogs.isEmpty()
          && (analysisContext == null
                 || ExecutionStatus.isFinalStatus(workflowService.getExecutionStatus(
                        analysisContext.getAppId(), analysisContext.getStateExecutionId())))) {
        CVActivityLog placeholderActivityLog = CVActivityLog.builder()
                                                   .stateExecutionId(stateExecutionId)
                                                   .log(EXECUTION_LOGS_NOT_AVAILABLE)
                                                   .logLevel(LogLevel.INFO)
                                                   .createdAt(System.currentTimeMillis())
                                                   .lastUpdatedAt(System.currentTimeMillis())
                                                   .build();
        cvActivityLogs = new ArrayList<>();
        cvActivityLogs.add(placeholderActivityLog);
      }
    } else {
      cvActivityLogs = findByCVConfigId(cvConfigId, startTimeMs, endTimeMs);
    }
    return cvActivityLogs;
  }

  private class LoggerImpl implements Logger {
    private String cvConfigId;
    private long dataCollectionMinute;
    private String stateExecutionId;
    LoggerImpl(String cvConfigId, long dataCollectionMinute) {
      Preconditions.checkNotNull(cvConfigId);
      this.cvConfigId = cvConfigId;
      this.dataCollectionMinute = dataCollectionMinute;
    }

    LoggerImpl(String stateExecutionId) {
      Preconditions.checkNotNull(stateExecutionId);
      this.stateExecutionId = stateExecutionId;
    }

    @Override
    public void log(LogLevel logLevel, String log, long... timestampParams) {
      CVActivityLog cvActivityLog =
          CVActivityLog.builder()
              .cvConfigId(cvConfigId)
              .stateExecutionId(stateExecutionId)
              .dataCollectionMinute(dataCollectionMinute)
              .log(log)
              .logLevel(logLevel)
              .timestampParams(Arrays.stream(timestampParams).boxed().collect(Collectors.toList()))
              .build();
      wingsPersistence.save(cvActivityLog);
    }
  }
}
