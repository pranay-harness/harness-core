/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.validation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DelegateValidateTask {
  List<DelegateConnectionResult> validationResults();
  List<String> getCriteria();
}
