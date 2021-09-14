/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.tasks.ProgressData;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface DelegateProgressData extends ProgressData {}
