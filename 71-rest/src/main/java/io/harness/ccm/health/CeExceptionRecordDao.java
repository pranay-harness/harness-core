package io.harness.ccm.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.health.CeExceptionRecord.CeExceptionRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;

@Slf4j
@Singleton
public class CeExceptionRecordDao {
  @Inject private HPersistence persistence;

  public String save(CeExceptionRecord exception) {
    return persistence.save(exception);
  }

  public CeExceptionRecord getLatestException(String accountId, String clusterId) {
    return persistence.createQuery(CeExceptionRecord.class)
        .field(CeExceptionRecordKeys.accountId)
        .equal(accountId)
        .field(CeExceptionRecordKeys.clusterId)
        .equal(clusterId)
        .order(Sort.descending(CeExceptionRecordKeys.createdAt))
        .get();
  }
}
