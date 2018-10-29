package io.harness.limits.checker.rate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.List;

/**
 * This collections tracks the usage for rate limiting purposes.
 */
@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "usageBuckets", noClassnameStored = true)
@Indexes(@Index(fields = @Field("key"), options = @IndexOptions(name = "key_idx", unique = true)))
public class UsageBucket extends Base {
  // key which uniquely identifies this bucket
  private String key;

  /**
   * List of times when bucket is accessed in current window.
   * So, if you are trying to rate limit an API, you'd update this bucket every time the API is hit.
   * See {@link MongoSlidingWindowRateLimitChecker#checkAndConsume} for example.
   */
  private List<Long> accessTimes;

  // morphia expects a no-args constructor
  private UsageBucket() {
    this.key = null;
    this.accessTimes = null;
  }
}
