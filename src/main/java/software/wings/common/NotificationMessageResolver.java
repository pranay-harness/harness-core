package software.wings.common;

import org.apache.commons.lang3.text.StrSubstitutor;
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;

import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 7/25/16.
 */
@Singleton
public class NotificationMessageResolver {
  private Pattern placeHolderPattern = Pattern.compile("\\$\\{.+?\\}");

  public static final String CHANGE_NOTIFICATION_TEMPLATE = "There's a <a href=${URL}>change</a> scheduled for ${DATE}";
  public static final String WORKFLOW_FAILURE_NOTIFICATION_TEMPLATE =
      "There are failures in the orchestrated workflow, <a href=${URL}>${NAME}</a>";
  public static final String ARTIFACT_APPROVAL_NOTIFICATION_TEMPLATE =
      "Artifact <a href=${URL}>${NAME}</a> is waiting for approval";

  /**
   * Gets decorated notification message.
   *
   * @param templateText the template text
   * @param params       the params
   * @return the decorated notification message
   */
  public String getDecoratedNotificationMessage(String templateText, Map<String, String> params) {
    if (templateText == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "message", "Template text can not be null");
    }
    templateText = StrSubstitutor.replace(templateText, params);
    validate(templateText);
    return templateText;
  }

  private void validate(String templateText) {
    if (placeHolderPattern.matcher(templateText).find()) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "message", "Incomplete placeholder replacement.");
    }
  }
}
