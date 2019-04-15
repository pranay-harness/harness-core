package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class TerraformValidation extends AbstractDelegateValidateTask {
  public TerraformValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    // TODO: check for git too
    ProcessExecutor processExecutor = new ProcessExecutor().command("/bin/sh", "-c", "terraform --version");
    boolean valid = false;
    try {
      final ProcessResult result = processExecutor.execute();
      valid = result.getExitValue() == 0;
    } catch (Exception e) {
      logger.error("Checking terraform version threw exception", e);
    }

    return singletonList(DelegateConnectionResult.builder().criteria("terraform").validated(valid).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList("Please ensure that terraform is installed on the delegate.");
  }
}
