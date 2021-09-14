/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import com.google.common.collect.Sets;
import java.util.Set;

public class EntityNameValidationMigration_All_01 extends EntityNameValidationMigration {
  private static Set<String> accountsToSkip = Sets.newHashSet("uUVzz7AsT6GugzxP80wlHg", // NYL
      "jKRddGK-R3GTbWHTW3GSag", // NCR
      "bwBVO7N0RmKltRhTjk101A", // iHerb
      "AOg9T42HTSq26LtpHm9YTg" // Opengov
  );

  @Override
  protected boolean skipAccount(String accountId) {
    return accountsToSkip.contains(accountId);
  }
}
