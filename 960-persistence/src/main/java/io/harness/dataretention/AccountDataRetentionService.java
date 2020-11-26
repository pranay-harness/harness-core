package io.harness.dataretention;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

@Singleton
@Slf4j
public class AccountDataRetentionService {
  public static final int BATCH = 1024;
  @Inject private HPersistence persistence;

  public <T extends AccountDataRetentionEntity> int corectValidUntilAccount(
      Class<? extends AccountDataRetentionEntity> clz, Map<String, Long> accounts, long now, long assureTo) {
    Query<T> query = persistence.createQuery((Class<T>) clz, excludeAuthority);
    query.order(Sort.ascending(AccountDataRetentionEntity.VALID_UNTIL_KEY));

    query.project(AccountDataRetentionEntity.ACCOUNT_ID_KEY, true);
    query.project(AccountDataRetentionEntity.VALID_UNTIL_KEY, true);
    query.project(AccountDataRetentionEntity.CREATED_AT_KEY, true);

    final DBCollection collection = persistence.getCollection(clz);
    BulkWriteOperation bulkWriteOperation = collection.initializeUnorderedBulkOperation();

    int i = 0;
    try (HIterator<T> entities = new HIterator<>(query.fetch())) {
      for (T entity : entities) {
        Long accountDataRetention = accounts.get(entity.getAccountId());
        // We do not have data retention specified for this account
        if (accountDataRetention == null) {
          continue;
        }

        long validUntil = (entity.getCreatedAt() == 0 ? now : entity.getCreatedAt()) + accountDataRetention;
        // The valid until value
        if (entity.getValidUntil() != null) {
          // If too far in the future, lets leave for later
          if (entity.getValidUntil().getTime() > assureTo) {
            break;
          }
          // Already correct, don't bother
          if (entity.getValidUntil().getTime() == validUntil) {
            continue;
          }
        }

        BasicDBObject set = new BasicDBObject(
            "$set", new BasicDBObject(AccountDataRetentionEntity.VALID_UNTIL_KEY, new Date(validUntil)));
        bulkWriteOperation.find(new BasicDBObject(Mapper.ID_KEY, entity.getUuid())).updateOne(set);

        if (++i % BATCH == 0) {
          bulkWriteOperation.execute();
          bulkWriteOperation = collection.initializeUnorderedBulkOperation();
          log.info("{}: {} updated", clz.getName(), i);
        }
      }
    }

    if (i % BATCH > 0) {
      bulkWriteOperation.execute();
    }

    return i;
  }
}
