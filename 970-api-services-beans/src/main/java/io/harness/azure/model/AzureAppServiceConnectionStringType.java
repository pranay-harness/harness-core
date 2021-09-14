/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.azure.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AzureAppServiceConnectionStringType {
  MYSQL("MySql"),
  SQL_SERVER("SQLServer"),
  SQL_AZURE("SQLAzure"),
  POSTGRE_SQL("PostgreSQL"),
  CUSTOM("Custom");

  private final String value;
  AzureAppServiceConnectionStringType(String value) {
    this.value = value;
  }

  @JsonValue
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
