package migrations.all;

import com.google.common.collect.Sets;

import java.util.Set;

public class EntityNameValidationMigration_All_03 extends EntityNameValidationMigration {
  private static Set<String> accountsToSkip = Sets.newHashSet("uUVzz7AsT6GugzxP80wlHg", // NYL
      "AOg9T42HTSq26LtpHm9YTg" // Opengov
  );

  protected boolean skipAccount(String accountId) {
    return accountsToSkip.contains(accountId);
  }
}
