package io.harness.event.grpc;

import com.google.inject.Inject;

import io.harness.ccm.health.CeExceptionRecord;
import io.harness.ccm.health.CeExceptionRecordDao;
import io.harness.event.payloads.CeExceptionMessage;

public class ExceptionMessageProcessor implements MessageProcessor {
  private final CeExceptionRecordDao ceK8SExceptionRecordDao;

  @Inject
  public ExceptionMessageProcessor(CeExceptionRecordDao ceK8SExceptionRecordDao) {
    this.ceK8SExceptionRecordDao = ceK8SExceptionRecordDao;
  }

  @Override
  public void process(PublishedMessage publishedMessage) {
    CeExceptionMessage exceptionMessage = (CeExceptionMessage) publishedMessage.getMessage();
    ceK8SExceptionRecordDao.save(CeExceptionRecord.builder()
                                     .accountId(publishedMessage.getAccountId())
                                     .clusterId(exceptionMessage.getClusterId())
                                     .message(exceptionMessage.getMessage())
                                     .build());
  }
}
