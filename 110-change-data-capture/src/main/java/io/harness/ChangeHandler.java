package io.harness;

import io.harness.changestreamsframework.ChangeEvent;

public interface ChangeHandler {
  boolean handleChange(ChangeEvent<?> changeEvent, String tableName, String[] fields);
}
