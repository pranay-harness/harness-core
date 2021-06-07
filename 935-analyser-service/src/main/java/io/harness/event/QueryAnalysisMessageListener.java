package io.harness.event;

import io.harness.eventsframework.consumer.Message;
import io.harness.ng.core.event.MessageListener;
import io.harness.repositories.QueryRecordsRepository;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryAnalysisMessageListener implements MessageListener {
  @Inject private QueryRecordsRepository queryRecordsRepository;

  @Override
  public boolean handleMessage(Message message) {
    log.debug("Message data : {}", message.getMessage().getData().toStringUtf8());
    String data = message.getMessage().getData().toStringUtf8();
    String queryResult = "{ \"queryExplainResult\":" + data + "}";
    QueryExplainResult queryExplainResult = JsonUtils.asObject(queryResult, QueryExplainResult.class);
    // Todo: get hash and version
    QueryRecordEntity queryRecordEntity = QueryRecordEntity.builder()
                                              .data(data)
                                              .explainResult(queryExplainResult)
                                              .hash("test")
                                              .version("version")
                                              .build();
    queryRecordsRepository.save(queryRecordEntity);
    return true;
  }
}
