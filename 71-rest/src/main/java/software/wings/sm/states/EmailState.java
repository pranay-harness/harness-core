/**
 *
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.EmailStateExecutionData;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class EmailState.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Attributes
@Slf4j
public class EmailState extends State {
  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

  @Attributes(required = true, title = "To") private String toAddress;
  @Attributes(title = "CC") private String ccAddress;
  @Attributes(required = true, title = "Subject") private String subject;
  @Attributes(title = "Body") private String body;
  @Attributes(title = "Ignore delivery failure?") private Boolean ignoreDeliveryFailure = true;

  @Transient @Inject private EmailNotificationService emailNotificationService;

  /**
   * Instantiates a new email state.
   *
   * @param name the name
   */
  public EmailState(String name) {
    super(name, StateType.EMAIL.name());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    EmailStateExecutionData emailStateExecutionData = anEmailStateExecutionData()
                                                          .withBody(body)
                                                          .withCcAddress(ccAddress)
                                                          .withToAddress(toAddress)
                                                          .withSubject(subject)
                                                          .build();
    try {
      String evaluatedTo = context.renderExpression(toAddress);
      String evaluatedCc = context.renderExpression(ccAddress);
      String evaluatedSubject = context.renderExpression(subject);
      String evaluatedBody = context.renderExpression(body);
      emailStateExecutionData.setSubject(evaluatedSubject);
      emailStateExecutionData.setBody(evaluatedBody);
      emailStateExecutionData.setToAddress(evaluatedTo);
      emailStateExecutionData.setCcAddress(evaluatedCc);
      logger.debug("Email Notification - subject:{}, body:{}", evaluatedSubject, evaluatedBody);
      emailNotificationService.send(EmailData.builder()
                                        .to(getEmailAddressList(evaluatedTo))
                                        .cc(getEmailAddressList(evaluatedCc))
                                        .subject(evaluatedSubject)
                                        .body(evaluatedBody)
                                        .accountId(context.getAccountId())
                                        .build());
      executionResponseBuilder.executionStatus(ExecutionStatus.SUCCESS);
    } catch (Exception e) {
      executionResponseBuilder.errorMessage(
          e.getCause() == null ? ExceptionUtils.getMessage(e) : ExceptionUtils.getMessage(e.getCause()));
      executionResponseBuilder.executionStatus(ignoreDeliveryFailure ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR);
      logger.error("Exception while sending email", e);
    }

    executionResponseBuilder.stateExecutionData(emailStateExecutionData);

    return executionResponseBuilder.build();
  }

  private List<String> getEmailAddressList(String address) {
    List addressList = new ArrayList();
    if (isNotEmpty(address)) {
      addressList.addAll(COMMA_SPLITTER.splitToList(address));
    }
    return addressList;
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets to address.
   *
   * @return the to address
   */
  public String getToAddress() {
    return toAddress;
  }

  /**
   * Sets to address.
   *
   * @param toAddress the to address
   */
  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  /**
   * Gets cc address.
   *
   * @return the cc address
   */
  public String getCcAddress() {
    return ccAddress;
  }

  /**
   * Sets cc address.
   *
   * @param ccAddress the cc address
   */
  public void setCcAddress(String ccAddress) {
    this.ccAddress = ccAddress;
  }

  /**
   * Gets subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets subject.
   *
   * @param subject the subject
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Gets body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets body.
   *
   * @param body the body
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Is ignore delivery failure boolean.
   *
   * @return the boolean
   */
  public Boolean isIgnoreDeliveryFailure() {
    return ignoreDeliveryFailure;
  }

  /**
   * Sets ignore delivery failure.
   *
   * @param ignoreDeliveryFailure the ignore delivery failure
   */
  public void setIgnoreDeliveryFailure(Boolean ignoreDeliveryFailure) {
    if (ignoreDeliveryFailure != null) {
      this.ignoreDeliveryFailure = ignoreDeliveryFailure;
    }
  }
}
