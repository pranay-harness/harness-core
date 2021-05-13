package io.harness.limits.checker.rate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import java.util.List;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * This collections tracks the usage for rate limiting purposes.
 */
@OwnedBy(PL)
@Value
@Entity(value = "usageBuckets", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "UsageBucketKeys")
@StoreIn(DbAliases.CG_MANAGER)
public class UsageBucket implements PersistentEntity {
  // key which uniquely identifies this bucket
  @Id private String key;

  /**
   * List of times when bucket is accessed in current window.
   * So, if you are trying to rate limit an API, you'd update this bucket every time the API is hit.
   * See {@link MongoSlidingWindowRateLimitChecker#checkAndConsume} for example.
   */
  private List<Long> accessTimes;
}
