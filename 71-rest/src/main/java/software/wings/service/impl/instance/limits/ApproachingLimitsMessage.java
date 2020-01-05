package software.wings.service.impl.instance.limits;

import io.harness.limits.ActionType;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@UtilityClass
public class ApproachingLimitsMessage {
  private static final Logger log = LoggerFactory.getLogger(ApproachingLimitsMessage.class);

  public static String warningMessage(int percent, ActionType actionType) {
    String template = null;
    switch (actionType) {
      case CREATE_APPLICATION:
        template =
            "You have consumed {}% of allowed Application creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      case CREATE_PIPELINE:
        template =
            "You have consumed {}% of allowed Pipeline creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      case CREATE_USER:
        template =
            "You have consumed {}% of allowed User creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      case CREATE_WORKFLOW:
        template =
            "You have consumed {}% of allowed Workflow creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      case CREATE_SERVICE:
        template =
            "You have consumed {}% of allowed Service creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      case CREATE_INFRA_PROVISIONER:
        template =
            "You have consumed {}% of allowed Infrastructure Provisioner creation limits. Please contact Harness Support to avoid any interruptions.";
        break;
      default:
        log.error(
            "No warning message configured. Please configure one or default message will be shown. ActionType: {}",
            actionType);
        return "Approaching resource usage limits. Please contact Harness Support to avoid any interruptions.";
    }

    return MessageFormatter.format(template, percent).getMessage();
  }
}
