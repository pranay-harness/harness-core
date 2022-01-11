/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.helpers.MessageFormatter;

/**
 * Encapsulates an action and the context (accountId) in which it is performed.
 */
@Value
@AllArgsConstructor
@OwnedBy(PL)
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
