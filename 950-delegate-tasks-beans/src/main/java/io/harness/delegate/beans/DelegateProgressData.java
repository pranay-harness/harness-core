package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.tasks.ProgressData;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
@OwnedBy(HarnessTeam.DEL)
public interface DelegateProgressData extends ProgressData {}
