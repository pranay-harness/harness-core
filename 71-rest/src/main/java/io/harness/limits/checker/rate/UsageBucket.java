package io.harness.limits.checker.rate;

import io.harness.persistence.PersistentEntity;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

/**
 * This collections tracks the usage for rate limiting purposes.
 */
@Value
@Entity(value = "usageBuckets", noClassnameStored = true)
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
