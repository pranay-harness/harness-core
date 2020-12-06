package io.harness.limits;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.helpers.MessageFormatter;

/**
 * Encapsulates an action and the context (accountId) in which it is performed.
 */
@Value
@AllArgsConstructor
public class Action {
  private String accountId;
  private ActionType actionType;

  public static Action fromKey(String key) {
    String[] parts = key.split(":");
    return new Action(parts[0], ActionType.valueOf(parts[1]));
  }

  /**
   * This is the key used in to keep track of count in counters collection/cache.
   * So this will be used as {{@link Counter#key}}
   */
  public String key() {
    return MessageFormatter.format("{}:{}", accountId, actionType).getMessage();
  }
}
