package io.harness;

import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegatelog.client.DelegateSelectionLogHttpClient;
import io.harness.network.SafeHttpCall;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateInfoHelper {
  // Todo: Remove dependency of orchestrationVisualization from 120-ng-manager
  @Inject(optional = true) private DelegateSelectionLogHttpClient delegateSelectionLogHttpClient;

  public List<DelegateSelectionLogParams> getDelegateInformationForGivenTask(
      List<ExecutableResponse> executableResponses, ExecutionMode executionMode, String accountId) {
    if (executionMode == ExecutionMode.TASK || executionMode == ExecutionMode.TASK_CHAIN) {
      return executableResponses.stream()
          .filter(executableResponse -> executableResponse.hasTaskChain() || executableResponse.hasTask())
          .map(executableResponse -> {
            try {
              if (executableResponse.hasTask()) {
                return SafeHttpCall
                    .execute(delegateSelectionLogHttpClient.getDelegateInfo(
                        accountId, executableResponse.getTask().getTaskId()))
                    .getResource();
              }
              return SafeHttpCall
                  .execute(delegateSelectionLogHttpClient.getDelegateInfo(
                      accountId, executableResponse.getTaskChain().getTaskId()))
                  .getResource();
            } catch (Exception exception) {
              log.error("Not able to talk to delegate service. Ignoring delegate Information");
            }
            return null;
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }
}
