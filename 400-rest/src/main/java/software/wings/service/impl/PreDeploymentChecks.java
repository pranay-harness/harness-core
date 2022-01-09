/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.FEATURE_UNAVAILABLE;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.WingsException;

import software.wings.beans.AccountType;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PreDeploymentChecks {
  private AccountService accountService;
  private WorkflowPreDeploymentValidator workflowPreDeploymentValidator;
  private PipelinePreDeploymentValidator pipelinePreDeploymentValidator;

  @Inject
  public PreDeploymentChecks(AccountService accountService,
      WorkflowPreDeploymentValidator workflowPreDeploymentValidator,
      PipelinePreDeploymentValidator pipelinePreDeploymentValidator) {
    this.accountService = accountService;
    this.workflowPreDeploymentValidator = workflowPreDeploymentValidator;
    this.pipelinePreDeploymentValidator = pipelinePreDeploymentValidator;
  }

  void checkIfWorkflowUsingRestrictedFeatures(@NotNull Workflow workflow) {
    String accountType = accountService.getAccountType(workflow.getAccountId()).orElse(AccountType.PAID);
    List<ValidationError> validationErrorList = workflowPreDeploymentValidator.validate(accountType, workflow);
    if (isNotEmpty(validationErrorList)) {
      String validationMessage = validationErrorList.get(0).getMessage();
      log.warn("Pre-deployment restricted features check failed for workflowId ={} with reason={} ", workflow.getUuid(),
          validationMessage);
      throw new WingsException(FEATURE_UNAVAILABLE, validationMessage, USER).addParam("message", validationMessage);
    }
  }

  void checkIfPipelineUsingRestrictedFeatures(@NotNull Pipeline pipeline) {
    String accountType = accountService.getAccountType(pipeline.getAccountId()).orElse(AccountType.PAID);
    List<ValidationError> validationErrorList = pipelinePreDeploymentValidator.validate(accountType, pipeline);
    if (isNotEmpty(validationErrorList)) {
      String validationMessage = validationErrorList.get(0).getMessage();
      log.warn("Pre-deployment restricted features check failed for pipelinedId ={} with reason={} ",
          pipeline.getUuid(), validationMessage);
      throw new WingsException(FEATURE_UNAVAILABLE, validationMessage, WingsException.USER);
    }
  }
}
