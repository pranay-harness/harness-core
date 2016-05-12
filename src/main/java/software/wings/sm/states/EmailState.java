/**
 *
 */
package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * @author Rishi
 *
 */
public class EmailState extends State {
  private static final long serialVersionUID = 1L;
  private String toAddress;
  private String ccAddress;
  private String subject;
  private String body;

  public EmailState(String name) {
    super(name, StateType.EMAIL.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    // TODO Auto-generated method stub
    return null;
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
}
