package software.wings.common;

import org.apache.commons.lang3.text.StrSubstitutor;
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 7/25/16.
 */
public class NotificationMessageResolver {
  /**
   * The constant ENTITY_CREATE_NOTIFICATION.
   */
  public static final String ENTITY_CREATE_NOTIFICATION = "A new ${ENTITY_TYPE} ${ENTITY_NAME} is created.";
  /**
   * The constant ENTITY_DELETE_NOTIFICATION.
   */
  public static final String ENTITY_DELETE_NOTIFICATION = "${ENTITY_TYPE} ${ENTITY_NAME} is deleted.";
  /**
   * The constant ADD_HOST_NOTIFICATION.
   */
  public static final String ADD_INFRA_HOST_NOTIFICATION = "${COUNT} new hosts added in ${INFRA_NAME} Infrastructure.";

  /**
   * The constant ADD_HOST_NOTIFICATION.
   */
  public static final String ADD_HOST_NOTIFICATION = "${COUNT} new hosts added in ${ENV_NAME} environment.";
  /**
   * The constant HOST_DELETE_NOTIFICATION.
   */
  public static final String HOST_DELETE_NOTIFICATION = "A host ${HOST_NAME} deleted from ${ENV_NAME} environment.";
  /**
   * The constant DEPLOYMENT_COMPLETED_NOTIFICATION.
   */
  public static final String DEPLOYMENT_COMPLETED_NOTIFICATION =
      "${DATE} : Deployment {NAME} completed on ${HOST_COUNT} in ${ENV_NAME} environment.";
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
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "message", "Template text can not be null");
    }
    templateText = StrSubstitutor.replace(templateText, params);
    validate(templateText);
    return templateText;
  }

  private static void validate(String templateText) {
    if (placeHolderPattern.matcher(templateText).find()) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "message", "Incomplete placeholder replacement.");
    }
  }
}
