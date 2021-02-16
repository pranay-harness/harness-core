package io.harness;

import io.harness.changestreamsframework.ChangeEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeScaleDBChangeHandler implements ChangeHandler {
  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent, String tableName, String[] fields) {
    return false;
  }
}
