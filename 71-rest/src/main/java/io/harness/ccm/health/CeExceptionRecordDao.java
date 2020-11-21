package io.harness.ccm.health;

import io.harness.ccm.health.CeExceptionRecord.CeExceptionRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;

@Slf4j
@Singleton
public class CeExceptionRecordDao {
  @Inject private HPersistence persistence;

  public String save(CeExceptionRecord exception) {
    return persistence.save(exception);
  }

  public CeExceptionRecord getRecentException(String accountId, String clusterId, long recentTimestamp) {
    return persistence.createQuery(CeExceptionRecord.class)
        .field(CeExceptionRecordKeys.accountId)
        .equal(accountId)
        .field(CeExceptionRecordKeys.clusterId)
        .equal(clusterId)
        .field(CeExceptionRecordKeys.createdAt)
        .greaterThanOrEq(recentTimestamp)
        .order(Sort.descending(CeExceptionRecordKeys.createdAt))
        .get();
  }
}
