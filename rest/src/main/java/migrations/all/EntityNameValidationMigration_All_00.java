package migrations.all;

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

  protected boolean skipAccount(String accountId) {
    return accountsToSkip.contains(accountId);
  }
}