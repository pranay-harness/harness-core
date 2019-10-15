package io.harness.batch.processing.billing.reader;

import com.google.inject.Singleton;

import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@Singleton
public class InstanceDataMongoEventReader implements InstanceDataEventReader {
  private static final int READER_BATCH_SIZE = 100;
  @Autowired private MongoTemplate mongoTemplate;

  @Override
  public ItemReader<InstanceData> getEventReader(Long startDate, Long endDate) {
    MongoItemReader<InstanceData> reader = new MongoItemReader<>();
    reader.setCollection("instanceData");
    reader.setTemplate(mongoTemplate);
    reader.setTargetType(InstanceData.class);
    Query query = new Query();
    query.with(new Sort(Sort.Direction.ASC, InstanceDataKeys.usageStartTime));
    reader.setQuery(query);
    reader.setPageSize(READER_BATCH_SIZE);
    return reader;
  }
}
