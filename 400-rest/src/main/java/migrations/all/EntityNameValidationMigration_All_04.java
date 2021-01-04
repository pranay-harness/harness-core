package migrations.all;

public class EntityNameValidationMigration_All_04 extends EntityNameValidationMigration {
  @Override
  protected boolean skipAccount(String accountId) {
    return false;
  }
}
