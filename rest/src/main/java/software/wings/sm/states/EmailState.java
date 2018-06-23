/**
 *
 */

package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.EmailStateExecutionData;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class EmailState.
 *
 * @author Rishi
 */
@Attributes
public class EmailState extends State {
  private static final Logger logger = LoggerFactory.getLogger(EmailState.class);

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
    ExecutionResponse executionResponse = new ExecutionResponse();
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
                                        .to(getEmailAddressList(toAddress))
                                        .cc(getEmailAddressList(ccAddress))
                                        .subject(evaluatedSubject)
                                        .body(evaluatedBody)
                                        .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                        .build());
      executionResponse.setExecutionStatus(ExecutionStatus.SUCCESS);
    } catch (Exception e) {
      executionResponse.setErrorMessage(e.getCause() == null ? Misc.getMessage(e) : Misc.getMessage(e.getCause()));
      executionResponse.setExecutionStatus(ignoreDeliveryFailure ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR);
      logger.error("Exception while sending email", e);
    }

    executionResponse.setStateExecutionData(emailStateExecutionData);

    return executionResponse;
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

  public Boolean getIgnoreDeliveryFailure() {
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
