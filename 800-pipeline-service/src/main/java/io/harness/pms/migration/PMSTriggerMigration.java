package io.harness.pms.migration;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity.NGTriggerEntityKeys;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class PMSTriggerMigration implements NGMigration {
  @Inject private final MongoTemplate mongoTemplate;
  private final RetryPolicy<Object> updateRetryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed updating Trigger; attempt: {}", "[Failed]: Failed updating Trigger; attempt: {}",
      ImmutableList.of(OptimisticLockingFailureException.class, DuplicateKeyException.class), Duration.ofSeconds(1), 3,
      log);

  @Override
  public void migrate() {
    int pageIdx = 0;
    int pageSize = 20;
    int maxUsers = 10000;
    int maxPages = maxUsers / pageSize;

    while (pageIdx < maxPages) {
      Pageable pageable = PageRequest.of(pageIdx, pageSize);
      Query query = new Query().with(pageable);
      List<NGTriggerEntity> triggers = mongoTemplate.find(query, NGTriggerEntity.class);
      if (triggers.isEmpty()) {
        break;
      }
      for (NGTriggerEntity ngTriggerEntity : triggers) {
        String triggerYaml = ngTriggerEntity.getYaml();
        String updatedYaml = triggerYaml.replace("type: branch", "type: PR")
                                 .replace("branch: <+trigger.branch>", "number: <+trigger.prNumber>");

        Update update = new Update();
        update.set(NGTriggerEntityKeys.yaml, updatedYaml);

        Query query1 = new Query(Criteria.where(NGTriggerEntityKeys.uuid).is(ngTriggerEntity.getUuid()));
        Failsafe.with(updateRetryPolicy)
            .get(()
                     -> mongoTemplate.findAndModify(
                         query1, update, new FindAndModifyOptions().returnNew(true), NGTriggerEntity.class));
      }

      pageIdx++;
      if (pageIdx % (maxPages / 5) == 0) {
        log.info("NGTrigger migration in process...");
      }
    }
  }
}
