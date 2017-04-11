package software.wings.common;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.exception.WingsException;
import software.wings.utils.YamlUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 7/25/16.
 */
@Singleton
public class NotificationMessageResolver {
  private Map<String, ChannelTemplate> templateMap = new HashMap<>();
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public enum NotificationMessageType {
    ENTITY_CREATE_NOTIFICATION,
    ENTITY_DELETE_NOTIFICATION,
    WORKFLOW_SUCCESSFUL_NOTIFICATION,
    WORKFLOW_PAUSED_NOTIFICATION,
    WORKFLOW_FAILED_NOTIFICATION,
    WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION,
    WORKFLOW_PHASE_PAUSED_NOTIFICATION,
    WORKFLOW_PHASE_FAILED_NOTIFICATION
  }

  private static Pattern placeHolderPattern = Pattern.compile("\\$\\{.+?\\}");

  /**
   * Gets decorated notification message.
   *
   * @param templateText the template text
   * @param params       the params
   * @return the decorated notification message
   */
  public static String getDecoratedNotificationMessage(String templateText, Map<String, String> params) {
    if (templateText == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "message", "Template text can not be null");
    }
    templateText = StrSubstitutor.replace(templateText, params);
    validate(templateText);
    return templateText;
  }

  private static void validate(String templateText) {
    if (placeHolderPattern.matcher(templateText).find()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "message", "Incomplete placeholder replacement.");
    }
  }

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

  public String getSlackTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getSlack();
  }

  public String getWebTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getWeb();
  }

  public EmailTemplate getEmailTemplate(String templateName) {
    return templateMap.getOrDefault(templateName, new ChannelTemplate()).getEmail();
  }

  public static class ChannelTemplate {
    private String web;
    private String slack;
    private EmailTemplate email;

    public String getWeb() {
      return web;
    }

    public void setWeb(String web) {
      this.web = web;
    }

    public String getSlack() {
      return slack;
    }

    public void setSlack(String slack) {
      this.slack = slack;
    }

    public EmailTemplate getEmail() {
      return email;
    }

    public void setEmail(EmailTemplate email) {
      this.email = email;
    }

    public static class EmailTemplate {
      private String subject;
      private String body;

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
  }
}
