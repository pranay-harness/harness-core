package io.harness.delegate.task.azure;

public enum AzureAppServiceConnectionStringsType {
  MYSQL("MySQL"),
  SQL_SERVER("SQLServer"),
  SQL_AZURE("SQLAzure"),
  POSTGRE_SQL("PostgreSQL"),
  CUSTOM("Custom");

  private final String displayName;
  AzureAppServiceConnectionStringsType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
