package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 6/13/16.
 */
public class MapperUtilsTest {
  /**
   * Map object.
   *
   * @throws Exception the exception
   */
  @Test
  public void mapObject() throws Exception {
    Map<String, Object> map = Maps.newLinkedHashMap();
    map.put("toAddress", "a@b.com");
    map.put("subject", "test");
    map.put("body", "test");

    EmailState emailState = new EmailState("id");
    MapperUtils.mapObject(map, emailState);
    assertThat(emailState)
        .extracting(EmailState::getToAddress, EmailState::getSubject, EmailState::getBody, EmailState::getName)
        .containsExactly("a@b.com", "test", "test", "id");
  }

  @Test
  public void mapSomeFields() throws Exception {
    EmailState emailState = new EmailState("name1");
    emailState.setBody("body1");

    Map<String, Object> map = new HashMap<>();
    map.put("toAddress", "toAddress1");
    map.put("ccAddress", "ccAddress1");

    MapperUtils.mapObject(map, emailState);

    assertThat(emailState)
        .extracting("name", "body", "toAddress", "ccAddress", "subject")
        .containsExactly("name1", "body1", "toAddress1", "ccAddress1", null);
  }

  /**
   * The Class EmailState.
   *
   * @author Rishi
   */
  public class EmailState {
    private String name;
    private String toAddress;
    private String ccAddress;
    private String subject;
    private String body;
    private Boolean ignoreDeliveryFailure = true;

    public EmailState(String name) {
      this.name = name;
    }

    /**
     * Getter for property 'name'.
     *
     * @return Value for property 'name'.
     */
    public String getName() {
      return name;
    }

    /**
     * Setter for property 'name'.
     *
     * @param name Value to set for property 'name'.
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Getter for property 'toAddress'.
     *
     * @return Value for property 'toAddress'.
     */
    public String getToAddress() {
      return toAddress;
    }

    /**
     * Setter for property 'toAddress'.
     *
     * @param toAddress Value to set for property 'toAddress'.
     */
    public void setToAddress(String toAddress) {
      this.toAddress = toAddress;
    }

    /**
     * Getter for property 'ccAddress'.
     *
     * @return Value for property 'ccAddress'.
     */
    public String getCcAddress() {
      return ccAddress;
    }

    /**
     * Setter for property 'ccAddress'.
     *
     * @param ccAddress Value to set for property 'ccAddress'.
     */
    public void setCcAddress(String ccAddress) {
      this.ccAddress = ccAddress;
    }

    /**
     * Getter for property 'subject'.
     *
     * @return Value for property 'subject'.
     */
    public String getSubject() {
      return subject;
    }

    /**
     * Setter for property 'subject'.
     *
     * @param subject Value to set for property 'subject'.
     */
    public void setSubject(String subject) {
      this.subject = subject;
    }

    /**
     * Getter for property 'body'.
     *
     * @return Value for property 'body'.
     */
    public String getBody() {
      return body;
    }

    /**
     * Setter for property 'body'.
     *
     * @param body Value to set for property 'body'.
     */
    public void setBody(String body) {
      this.body = body;
    }

    /**
     * Getter for property 'ignoreDeliveryFailure'.
     *
     * @return Value for property 'ignoreDeliveryFailure'.
     */
    public Boolean getIgnoreDeliveryFailure() {
      return ignoreDeliveryFailure;
    }

    /**
     * Setter for property 'ignoreDeliveryFailure'.
     *
     * @param ignoreDeliveryFailure Value to set for property 'ignoreDeliveryFailure'.
     */
    public void setIgnoreDeliveryFailure(Boolean ignoreDeliveryFailure) {
      this.ignoreDeliveryFailure = ignoreDeliveryFailure;
    }
  }
}
