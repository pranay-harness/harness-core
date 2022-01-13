/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Account type.
 * @author rktummala on 08/29/18
 */
// Note: This is intentionally not made enum
public interface AccountType {
  Logger log = LoggerFactory.getLogger(AccountType.class);

  String TRIAL = "TRIAL";
  String PAID = "PAID";
  String COMMUNITY = "COMMUNITY";
  String ESSENTIALS = "ESSENTIALS";

  Set<String> allAccountTypes = ImmutableSet.of(TRIAL, ESSENTIALS, COMMUNITY, PAID);

  static boolean isValid(String type) {
    if (isEmpty(type)) {
      return false;
    }

    switch (StringUtils.upperCase(type)) {
      case ESSENTIALS:
      case TRIAL:
      case PAID:
      case COMMUNITY:
        return true;
      default:
        return false;
    }
  }

  static boolean isCommunity(String type) {
    if (isEmpty(type)) {
      return false;
    }
    if (!isValid(type)) {
      log.warn("[INVALID_ACCOUNT_TYPE] type={}", type);
      return false;
    }

    return StringUtils.equalsIgnoreCase(COMMUNITY, type);
  }
}
