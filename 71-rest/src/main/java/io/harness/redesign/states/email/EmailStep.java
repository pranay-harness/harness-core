package io.harness.redesign.states.email;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Redesign
@Produces(Step.class)
@Slf4j
public class EmailStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("EMAIL").build();

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
  private static final String ACCOUNT_ID = "accountId";

  @Transient @Inject private EmailNotificationService emailNotificationService;

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters parameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    EmailStepParameters stepParameters = (EmailStepParameters) parameters;
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    try {
      logger.debug("Email Notification - subject:{}, body:{}", stepParameters.getSubject(), stepParameters.getBody());
      emailNotificationService.send(EmailData.builder()
                                        .to(getEmailAddressList(stepParameters.getToAddress()))
                                        .cc(getEmailAddressList(stepParameters.getCcAddress()))
                                        .subject(stepParameters.getSubject())
                                        .body(stepParameters.getBody())
                                        .accountId((String) ambiance.getInputArgs().get(ACCOUNT_ID))
                                        .build());
      stepResponseBuilder.status(NodeExecutionStatus.SUCCEEDED);
    } catch (Exception e) {
      stepResponseBuilder
          .failureInfo(StepResponse.FailureInfo.builder()
                           .errorMessage(e.getCause() == null ? ExceptionUtils.getMessage(e)
                                                              : ExceptionUtils.getMessage(e.getCause()))
                           .build())
          .status(
              stepParameters.isIgnoreDeliveryFailure() ? NodeExecutionStatus.SUCCEEDED : NodeExecutionStatus.FAILED);
      logger.error("Exception while sending email", e);
    }
    return stepResponseBuilder.build();
  }

  @Override
  public StepType getType() {
    return STEP_TYPE;
  }

  private List<String> getEmailAddressList(String address) {
    List<String> addressList = new ArrayList<>();
    if (isNotEmpty(address)) {
      addressList.addAll(COMMA_SPLITTER.splitToList(address));
    }
    return addressList;
  }
}
