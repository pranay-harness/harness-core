package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;

@OwnedBy(HarnessTeam.CE)
public interface ChangeHandler {
  boolean handleChange(ChangeEvent<?> changeEvent, String tableName, String[] fields);
}
