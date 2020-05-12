package software.wings.delegatetasks.validation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import software.wings.beans.SftpConfig;
import software.wings.service.impl.SftpHelperService;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public class SftpValidation extends AbstractDelegateValidateTask {
  @Inject SftpHelperService sftpHelperService;
  public SftpValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof SftpConfig)
                             .map(config -> ((SftpConfig) config).getSftpUrl())
                             .findFirst()
                             .orElse(null));
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    try {
      String criteria = getCriteria().get(0);
      String connectionHost = sftpHelperService.getSFTPConnectionHost(criteria);
      return singletonList(DelegateConnectionResult.builder()
                               .criteria(criteria)
                               .validated(sftpHelperService.isConnectibleSFTPServer(connectionHost))
                               .build());
    } catch (Exception e) {
      return emptyList();
    }
  }
}
