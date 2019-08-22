package io.harness.batch.processing.reader;

import io.harness.event.grpc.PublishedMessage;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@Qualifier("mongoEventReader")
@Slf4j
public class MongoEventReader implements EventReader {
  @Autowired private MongoTemplate mongoTemplate;

  @Override
  public ItemReader<PublishedMessage> getEventReader(String messageType, Long startDate, Long endDate) {
    MongoItemReader<PublishedMessage> reader = new MongoItemReader<>();
    reader.setCollection("publishedMessages");
    reader.setTemplate(mongoTemplate);
    reader.setTargetType(PublishedMessage.class);
    Query query = new Query();
    query.addCriteria(Criteria.where(PublishedMessageKeys.type)
                          .is(messageType)
                          .andOperator(Criteria.where(PublishedMessageKeys.createdAt).gte(startDate),
                              Criteria.where(PublishedMessageKeys.createdAt).lte(endDate)));
    query.with(new Sort(Sort.Direction.ASC, PublishedMessageKeys.createdAt));
    reader.setQuery(query);
    reader.setPageSize(READER_BATCH_SIZE);
    return reader;
  }
}
