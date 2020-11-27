package io.harness.eventsframework;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventFrameworkConstants {
  // Maintain the channel names here instead of hardcoding at various places
  // Also, for channels which are dynamically created, please ensure that the
  // prefixes are picked up from this file
  public static final String PROJECT_UPDATE_CHANNEL = "project_update";
}
