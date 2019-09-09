package software.wings.beans.trigger;

import software.wings.beans.trigger.WebhookSource.WebhookEvent;

import java.util.List;

public interface GitEventType { List<WebhookEvent> getEventTypes(); }
