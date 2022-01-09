/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import com.google.common.collect.Sets;
import java.util.Set;

public class EntityNameValidationMigration_All_03 extends EntityNameValidationMigration {
  private static Set<String> accountsToSkip = Sets.newHashSet("uUVzz7AsT6GugzxP80wlHg", // NYL
      "AOg9T42HTSq26LtpHm9YTg" // Opengov
  );

  @Override
  protected boolean skipAccount(String accountId) {
    return accountsToSkip.contains(accountId);
  }
}
