package io.harness.repositories.executions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.pms.plan.execution.AccountExecutionInfo;
import io.harness.pms.plan.execution.AccountExecutionMetadata;
import io.harness.pms.plan.execution.AccountExecutionMetadata.AccountExecutionMetadataKeys;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class AccountExecutionMetadataRepositoryCustomImpl implements AccountExecutionMetadataRepositoryCustom {
  private static final String LOCK_NAME_PREFIX = "ACCOUNT_EXECUTION_INFO_";
  private final MongoTemplate mongoTemplate;
  private final PersistentLocker persistentLocker;

  @Override
  public void updateAccountExecutionMetadata(String accountId, Set<String> moduleNames, Long startTS) {
    Criteria criteria = Criteria.where(AccountExecutionMetadataKeys.accountId).is(accountId);
    Query query = new Query(criteria);
    // Since there can be parallel executions for a given account, update after taking a lock
    try (
        AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(LOCK_NAME_PREFIX + accountId, Duration.ofMinutes(2))) {
      if (lock == null) {
        throw new InvalidRequestException("Could not acquire lock");
      }

      AccountExecutionMetadata accountExecutionMetadata = mongoTemplate.findOne(query, AccountExecutionMetadata.class);
      // If there is no entry, then create an entry in the db for the given account
      if (accountExecutionMetadata == null) {
        Map<String, Long> moduleToExecutionCount = new HashMap<>();
        for (String module : moduleNames) {
          moduleToExecutionCount.put(module, 1L);
        }
        AccountExecutionMetadata newAccountExecutionMetadata = AccountExecutionMetadata.builder()
                                                                   .accountId(accountId)
                                                                   .moduleToExecutionCount(moduleToExecutionCount)
                                                                   .build();
        mongoTemplate.save(newAccountExecutionMetadata);
        return;
      }
      for (String module : moduleNames) {
        // increase total count for given module
        long currentCount = accountExecutionMetadata.getModuleToExecutionCount().get(module);
        accountExecutionMetadata.getModuleToExecutionCount().put(module, currentCount + 1);

        // Increase count per month
        AccountExecutionInfo accountExecutionInfo;
        if (accountExecutionMetadata.getModuleToExecutionInfoMap() != null) {
          accountExecutionInfo = accountExecutionMetadata.getModuleToExecutionInfoMap().getOrDefault(
              module, AccountExecutionInfo.builder().build());
        } else {
          accountExecutionInfo = AccountExecutionInfo.builder().build();
          accountExecutionMetadata.setModuleToExecutionInfoMap(new HashMap<>());
        }
        LocalDate startDate = Instant.ofEpochMilli(startTS).atZone(ZoneId.systemDefault()).toLocalDate();
        Long countOfMonth = accountExecutionInfo.getCountPerMonth().getOrDefault(
            YearMonth.of(startDate.getYear(), startDate.getMonth()).toString(), 0L);
        countOfMonth = countOfMonth + 1;
        accountExecutionInfo.getCountPerMonth().put(
            YearMonth.of(startDate.getYear(), startDate.getMonth()).toString(), countOfMonth);
        accountExecutionMetadata.getModuleToExecutionInfoMap().put(module, accountExecutionInfo);
      }
      mongoTemplate.save(accountExecutionMetadata);
    }
  }
}
