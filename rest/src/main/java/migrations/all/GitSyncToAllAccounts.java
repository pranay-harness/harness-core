package migrations.all;

import migrations.Migration;

/**
 * Migration script to do a git sync to all accounts.
 * This is needed one time since we want to force a git sync due to field changes.
 * Going forward, the migration service runs git sync when there are migrations to be run.
 * @author rktummala on 4/5/18
 */
public class GitSyncToAllAccounts implements Migration {
  @Override
  public void migrate() {
    // do nothing
  }
}
