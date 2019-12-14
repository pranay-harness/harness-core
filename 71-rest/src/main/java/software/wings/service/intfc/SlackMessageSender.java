package software.wings.service.intfc;

import software.wings.beans.SlackMessage;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

public interface SlackMessageSender {
  @DelegateTaskType(TaskType.SLACK) void send(SlackMessage slackMessage, boolean forceProxyCallForDelegate);
}
