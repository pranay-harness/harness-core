package software.wings.common;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.exception.WingsException;
import software.wings.utils.YamlUtils;

import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 7/25/16.
 */
@Singleton
public class NotificationMessageResolver {
  private Map<String, ChannelTemplate> templateMap;
  private static final Logger logger = LoggerFactory.getLogger(NotificationMessageResolver.class);

  /**
   * The enum Notification message type.
   */
  public enum NotificationMessageType {
    ENTITY_CREATE_NOTIFICATION,
    ENTITY_DELETE_NOTIFICATION,
    ARTIFACT_APPROVAL_NOTIFICATION,
    ARTIFACT_APPROVAL_NOTIFICATION_STATUS,
    WORKFLOW_NOTIFICATION,
    WORKFLOW_PHASE_NOTIFICATION
  }

  private static Pattern placeHolderPattern = Pattern.compile("\\$\\{.+?}");

  /**
   * Gets decorated notification message.
   *
   * @param templateText the template text
   * @param params       the params
   * @return the decorated notification message
   */
  public static String getDecoratedNotificationMessage(String templateText, Map<String, String> params) {
    if (templateText == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Template text can not be null");
    }
    templateText = StrSubstitutor.replace(templateText, params);
    validate(templateText);
    return templateText;
  }

  private static void validate(String templateText) {
    if (placeHolderPattern.matcher(templateText).find()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Incomplete placeholder replacement.");
    }
  }

  /**
   * Instantiates a new Notification message resolver.
   *
   * @param yamlUtils the yaml utils
   */
  @Inject
  public NotificationMessageResolver(YamlUtils yamlUtils) {
    try {
      URL url = this.getClass().getResource(Constants.NOTIFICATION_TEMPLATE_PATH);
      String yaml = Resources.toString(url, Charsets.UTF_8);
      templateMap = yamlUtils.read(yaml, new TypeReference<Map<String, ChannelTemplate>>() {});
    } catch (Exception e) {
      logger.error("Error in initializing catalog", e);
      throw new WingsException(e);
    }
  }

  /**
   * Gets slack template.
   *
   * @param templateName the template name
   * @return the slack template
   */
  public String getSlackTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getSlack();
  }

  /**
   * Gets web template.
   *
   * @param templateName the template name
   * @return the web template
   */
  public String getWebTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getWeb();
  }

  /**
   * Gets email template.
   *
   * @param templateName the template name
   * @return the email template
   */
  public EmailTemplate getEmailTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getEmail();
  }

  /**
   * The type Channel template.
   */
  public static class ChannelTemplate {
    private String web;
    private String slack;
    private EmailTemplate email;

    /**
     * Gets web.
     *
     * @return the web
     */
    public String getWeb() {
      return web;
    }

    /**
     * Sets web.
     *
     * @param web the web
     */
    public void setWeb(String web) {
      this.web = web;
    }

    /**
     * Gets slack.
     *
     * @return the slack
     */
    public String getSlack() {
      return slack;
    }

    /**
     * Sets slack.
     *
     * @param slack the slack
     */
    public void setSlack(String slack) {
      this.slack = slack;
    }

    /**
     * Gets email.
     *
     * @return the email
     */
    public EmailTemplate getEmail() {
      return email;
    }

    /**
     * Sets email.
     *
     * @param email the email
     */
    public void setEmail(EmailTemplate email) {
      this.email = email;
    }

    /**
     * The type Email template.
     */
    public static class EmailTemplate {
      private String subject;
      private String body;

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
    }
  }
}
