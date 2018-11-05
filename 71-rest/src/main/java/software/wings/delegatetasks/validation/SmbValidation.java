package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import software.wings.beans.DelegateTask;
import software.wings.beans.SmbConfig;
import software.wings.service.impl.SmbHelperService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SmbValidation extends AbstractDelegateValidateTask {
  @Inject SmbHelperService smbHelperService;
  public SmbValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof SmbConfig)
                             .map(config -> smbHelperService.getSMBConnectionHost(((SmbConfig) config).getSmbUrl()))
                             .findFirst()
                             .orElse(null));
  }
}
