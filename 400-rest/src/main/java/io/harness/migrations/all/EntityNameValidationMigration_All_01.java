package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.common.collect.Sets;
import java.util.Set;

@TargetModule(Module._390_DB_MIGRATION)
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
