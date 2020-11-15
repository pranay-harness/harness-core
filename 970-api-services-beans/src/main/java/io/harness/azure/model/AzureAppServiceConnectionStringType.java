package io.harness.azure.model;

public enum AzureAppServiceConnectionStringType {
  MYSQL("MySQL"),
  SQL_SERVER("SQLServer"),
  SQL_AZURE("SQLAzure"),
  POSTGRE_SQL("PostgreSQL"),
  CUSTOM("Custom");

  private final String value;
  AzureAppServiceConnectionStringType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static AzureAppServiceConnectionStringType fromValue(String text) {
    for (AzureAppServiceConnectionStringType b : AzureAppServiceConnectionStringType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}
