package software.wings.service.impl.verification;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.CVActivityLogKeys;
import software.wings.verification.CVActivityLog.LogLevel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CVActivityLogServiceImpl implements CVActivityLogService {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<CVActivityLog> findByCVConfigId(String cvConfigId, long startTimeMilli, long endTimeMilli) {
    PageRequest<CVActivityLog> pageRequest = aPageRequest()
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
            .addFilter(CVActivityLogKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addOrder(CVActivityLogKeys.createdAt, OrderType.ASC)
            .build();
    return wingsPersistence.query(CVActivityLog.class, pageRequest, excludeAuthority).getResponse();
  }

  @Override
  public void saveActivityLogs(List<CVActivityLog> cvActivityLogs) {
    wingsPersistence.save(cvActivityLogs);
  }

  private class LoggerImpl implements Logger {
    private String cvConfigId;
    private long dataCollectionMinute;
    private String stateExecutionId;
    LoggerImpl(String cvConfigId, long dataCollectionMinute) {
      this.cvConfigId = cvConfigId;
      this.dataCollectionMinute = dataCollectionMinute;
    }

    LoggerImpl(String stateExecutionId) {
      this.stateExecutionId = stateExecutionId;
    }

    @Override
    public void log(LogLevel logLevel, String log, long... timestampParams) {
      if (featureFlagService.isGlobalEnabled(FeatureName.CV_ACTIVITY_LOG)) {
        if (stateExecutionId == null && cvConfigId == null) {
          // This check should be moved to findBy* but
          // keeping it inside feature flag for now so that we
          // don't have runtime exception when feature
          // flag is disabled.
          throw new RuntimeException("one of stateExecutionId or cvConfigId should be non null");
        }

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
}
