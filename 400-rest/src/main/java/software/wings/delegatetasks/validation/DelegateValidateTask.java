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
