/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import com.google.common.collect.Sets;
import java.util.Set;

public class EntityNameValidationMigration_All_00 extends EntityNameValidationMigration {
  private static Set<String> accountsToSkip = Sets.newHashSet("uUVzz7AsT6GugzxP80wlHg", // NYL
      "jKRddGK-R3GTbWHTW3GSag", // NCR
      "bwBVO7N0RmKltRhTjk101A", // iHerb
      "XEAgZ-j4RvirUgGObdd8-g", // Skyhigh
      "AOg9T42HTSq26LtpHm9YTg" // Opengov
  );
  // private static Set<String> accountsToSkip = emptySet();

  @Override
  protected boolean skipAccount(String accountId) {
    return accountsToSkip.contains(accountId);
  }
}
