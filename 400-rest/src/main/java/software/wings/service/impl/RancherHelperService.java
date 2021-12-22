package software.wings.service.impl;

import static io.harness.exception.WingsException.USER;

import static software.wings.service.impl.AssignDelegateServiceImpl.SCOPE_WILDCARD;

import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.KeyValuePair;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.RancherConfig;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.states.HttpState;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;

@Singleton
@Slf4j
public class RancherHelperService {
  @Inject private DelegateService delegateService;

  public boolean validateRancherConfig(RancherConfig rancherConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    StringBuffer urlBuffer = new StringBuffer(rancherConfig.getRancherUrl());
    urlBuffer.append("/v3/clusters");
    List<KeyValuePair> headers = new ArrayList<>();
    headers.add(KeyValuePair.builder()
                    .key(HttpHeaders.AUTHORIZATION)
                    .value("Bearer token-gr5wc:q6nnj62dmlrsjvfc5jnh68h8b56rg22csldssgwchrlcd8nw28567f")
                    .build());
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .method("GET")
                                                .url(urlBuffer.toString())
                                                .headers(headers)
                                                .socketTimeoutMillis(1000)
                                                .isCertValidationRequired(rancherConfig.isCertValidationRequired())
                                                .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(rancherConfig.getAccountId())
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, SCOPE_WILDCARD)
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(TaskType.HTTP.name())
                                              .parameters(new Object[] {httpTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(10))
                                              .build())
                                    .build();
    DelegateResponseData notifyResponseData;
    try {
      notifyResponseData = delegateService.executeTask(delegateTask);
      validateRancherDelegateResponse((HttpState.HttpStateExecutionResponse) notifyResponseData);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(e.getMessage(), USER);
    }
    return true;
  }

  private void validateRancherDelegateResponse(HttpState.HttpStateExecutionResponse delegateResponseData) {
    if (delegateResponseData.getExecutionStatus() != ExecutionStatus.SUCCESS) {
      throw new InvalidRequestException(delegateResponseData.getErrorMessage(), WingsException.USER);
    }
  }
}
