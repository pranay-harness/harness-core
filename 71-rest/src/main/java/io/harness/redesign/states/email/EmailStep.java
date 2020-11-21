package io.harness.redesign.states.email;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;

import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class EmailStep implements Step, SyncExecutable<EmailStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("EMAIL").build();

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
  private static final String ACCOUNT_ID = "accountId";

  @Transient @Inject private EmailNotificationService emailNotificationService;

  @Override
  public StepResponse executeSync(Ambiance ambiance, EmailStepParameters emailStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    try {
      log.debug(
          "Email Notification - subject:{}, body:{}", emailStepParameters.getSubject(), emailStepParameters.getBody());
      emailNotificationService.send(EmailData.builder()
                                        .to(getEmailAddressList(emailStepParameters.getToAddress()))
                                        .cc(getEmailAddressList(emailStepParameters.getCcAddress()))
                                        .subject(emailStepParameters.getSubject())
                                        .body(emailStepParameters.getBody())
                                        .accountId(ambiance.getSetupAbstractions().get(ACCOUNT_ID))
                                        .build());
      stepResponseBuilder.status(Status.SUCCEEDED);
    } catch (Exception e) {
      stepResponseBuilder
          .failureInfo(FailureInfo.builder()
                           .errorMessage(e.getCause() == null ? ExceptionUtils.getMessage(e)
                                                              : ExceptionUtils.getMessage(e.getCause()))
                           .build())
          .status(emailStepParameters.isIgnoreDeliveryFailure() ? Status.SUCCEEDED : Status.FAILED);
      log.error("Exception while sending email", e);
    }
    return stepResponseBuilder.build();
  }

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
