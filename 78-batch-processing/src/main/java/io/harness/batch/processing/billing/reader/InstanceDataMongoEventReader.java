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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;

@Slf4j
@Singleton
public class InstanceDataMongoEventReader implements InstanceDataEventReader {
  private static final int READER_BATCH_SIZE = 500;
  @Autowired private MongoTemplate mongoTemplate;

  @Override
  public ItemReader<InstanceData> getEventReader(String accountId, Long startDate, Long endDate) {
    MongoItemReader<InstanceData> reader = new MongoItemReader<>();
    reader.setCollection("instanceData");
    reader.setTemplate(mongoTemplate);
    reader.setTargetType(InstanceData.class);
    Query query = new Query();
    query.addCriteria(
        new Criteria()
            .andOperator(Criteria.where(InstanceDataKeys.accountId).is(accountId),
                Criteria.where(InstanceDataKeys.usageStartTime).exists(true))
            .orOperator(
                Criteria.where(InstanceDataKeys.usageStartTime)
                    .exists(true)
                    .andOperator(Criteria.where(InstanceDataKeys.usageStartTime)
                                     .lt(new Date(startDate))
                                     .orOperator(Criteria.where(InstanceDataKeys.usageStopTime).exists(false),
                                         Criteria.where(InstanceDataKeys.usageStopTime).gt(new Date(startDate)))),
                Criteria.where(InstanceDataKeys.usageStartTime)
                    .exists(true)
                    .andOperator(Criteria.where(InstanceDataKeys.usageStartTime).gte(new Date(startDate)),
                        Criteria.where(InstanceDataKeys.usageStartTime).lt(new Date(endDate)))));

    query.with(new Sort(Sort.Direction.ASC, InstanceDataKeys.usageStartTime));
    reader.setQuery(query);
    reader.setPageSize(READER_BATCH_SIZE);
    return reader;
  }
}
