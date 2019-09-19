package io.harness.delegate.service;

import com.google.inject.Inject;

import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.LogLevel;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DelegateCVActivityLogServiceImpl implements DelegateCVActivityLogService {
  @Inject private DelegateLogService delegateLogService;
  @Override
  public Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId,
      String suffix, long... suffixTimestampParams) {
    return new LoggerImpl(accountId, cvConfigId, dataCollectionMinute, stateExecutionId, suffix, suffixTimestampParams);
  }

  private class LoggerImpl implements Logger {
    private final String accountId;
    private final String suffix;
    private String cvConfigId;
    private long dataCollectionMinute;
    private String stateExecutionId;
    private List<Long> suffixTimestampParams;
    LoggerImpl(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId, String suffix,
        long... suffixTimestampParams) {
      this.cvConfigId = cvConfigId;
      this.dataCollectionMinute = dataCollectionMinute;
      this.accountId = accountId;
      this.stateExecutionId = stateExecutionId;
      this.suffix = suffix;
      this.suffixTimestampParams = Arrays.stream(suffixTimestampParams).boxed().collect(Collectors.toList());
    }

    @Override
    public void log(LogLevel logLevel, String log, long... timestampParams) {
      List<Long> timestampParamList = Arrays.stream(timestampParams).boxed().collect(Collectors.toList());
      timestampParamList.addAll(suffixTimestampParams);
      CVActivityLog activityLog = CVActivityLog.builder()
                                      .log("Delegate: " + log + suffix)
                                      .logLevel(logLevel)
                                      .cvConfigId(cvConfigId)
                                      .createdAt(Instant.now().toEpochMilli())
                                      .stateExecutionId(stateExecutionId)
                                      .dataCollectionMinute(dataCollectionMinute)
                                      .timestampParams(timestampParamList)
                                      .build();
      delegateLogService.save(accountId, activityLog);
    }
  }
}
