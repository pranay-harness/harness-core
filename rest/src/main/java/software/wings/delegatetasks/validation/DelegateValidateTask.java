package software.wings.delegatetasks.validation;

import java.util.List;

/**
 * Created by brett on 11/1/17
 */
public interface DelegateValidateTask extends Runnable {
  List<DelegateConnectionResult> validate();

  List<String> getCriteria();
}
