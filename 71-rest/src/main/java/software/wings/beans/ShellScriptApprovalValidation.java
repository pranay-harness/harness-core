package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.netty.util.NetUtil.LOCALHOST;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public class ShellScriptApprovalValidation extends AbstractDelegateValidateTask {
  public ShellScriptApprovalValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    return singletonList(DelegateConnectionResult.builder().validated(true).build());
  }

  @Override
  public List<String> getCriteria() {
    List<String> list = new ArrayList<>();
    list.add(LOCALHOST.getHostName());

    return list;
  }
}
