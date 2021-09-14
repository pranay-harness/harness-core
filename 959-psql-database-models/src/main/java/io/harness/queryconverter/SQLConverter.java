/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queryconverter;

import static io.harness.queryconverter.QueryHelper.throwInvalidIfNull;
import static io.harness.timescaledb.Public.PUBLIC;

import io.harness.queryconverter.dto.GridRequest;

import java.io.Serializable;
import java.util.List;
import lombok.NonNull;
import org.jooq.Field;
import org.jooq.Table;

public interface SQLConverter {
  List<? extends Serializable> convert(@NonNull GridRequest request) throws Exception;

  List<? extends Serializable> convert(@NonNull String tableName, GridRequest request) throws Exception;

  List<? extends Serializable> convert(Table<?> jooqTable, GridRequest request) throws Exception;

  List<? extends Serializable> convert(String tableName, GridRequest request, Class<? extends Serializable> fetchInto)
      throws Exception;

  List<? extends Serializable> convert(Table<?> jooqTable, GridRequest request, Class<? extends Serializable> fetchInto)
      throws Exception;

  static Field<?> getField(@NonNull String columnName, @NonNull String tableName) {
    Table<?> table = getTable(tableName);

    return getField(columnName, table);
  }

  static Table<?> getTable(@NonNull String tableName) {
    Table<?> table = PUBLIC.getTable(tableName.toLowerCase());

    throwInvalidIfNull(table, "table " + tableName.toLowerCase() + " doesnt exist in public schema");

    return table;
  }

  static Field<?> getField(@NonNull String columnName, @NonNull Table<?> table) {
    Field<?> field = table.field(columnName.toLowerCase());

    throwInvalidIfNull(field, "column " + columnName.toLowerCase() + " doesnt exist in table " + table.getName());

    return field;
  }
}
