package software.wings.beans.trigger;

import software.wings.beans.trigger.WebhookSource.EventType;

import java.util.List;

public interface GitEventType { List<EventType> getEventTypes(); }
