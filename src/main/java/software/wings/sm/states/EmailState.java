/**
 *
 */
package software.wings.sm.states;

import static software.wings.api.EmailStateExecutionData.Builder.anEmailStateExecutionData;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.EmailStateExecutionData;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.NotificationService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * @author Rishi
 */
@Attributes(title = "Email")
public class EmailState extends State {
  private static final Logger logger = LoggerFactory.getLogger(EmailState.class);

  private static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
  private static final long serialVersionUID = 1L;
  @Attributes(required = true) private String toAddress;
  @Attributes(required = true) private String ccAddress;
  @Attributes(required = true) private String subject;
  @Attributes(required = true) private String body;
  private boolean ignoreDeliveryFailure = true;

  @Inject private transient NotificationService<EmailData> emailNotificationService;

  public EmailState(String name) {
    super(name, StateType.EMAIL.name());
  }

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
      String evaluatedSubject = context.renderExpression(subject);
      String evaluatedBody = context.renderExpression(body);
      emailStateExecutionData.setSubject(evaluatedSubject);
      emailStateExecutionData.setBody(evaluatedBody);
      emailNotificationService.send(COMMA_SPLITTER.splitToList(toAddress), COMMA_SPLITTER.splitToList(ccAddress),
          evaluatedSubject, evaluatedBody);
      executionResponse.setExecutionStatus(ExecutionStatus.SUCCESS);
    } catch (Exception e) {
      executionResponse.setErrorMessage(e.getMessage());
      executionResponse.setExecutionStatus(ignoreDeliveryFailure ? ExecutionStatus.SUCCESS : ExecutionStatus.ERROR);
      logger.error("Exception while sending email: " + e);
    }

    executionResponse.setStateExecutionData(emailStateExecutionData);

    return executionResponse;
  }

  public String getToAddress() {
    return toAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  public String getCcAddress() {
    return ccAddress;
  }

  public void setCcAddress(String ccAddress) {
    this.ccAddress = ccAddress;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public boolean isIgnoreDeliveryFailure() {
    return ignoreDeliveryFailure;
  }

  public void setIgnoreDeliveryFailure(boolean ignoreDeliveryFailure) {
    this.ignoreDeliveryFailure = ignoreDeliveryFailure;
  }
}
