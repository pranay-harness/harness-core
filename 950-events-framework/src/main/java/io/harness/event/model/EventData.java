/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * @author rktummala
 */
@Data
@Builder
public class EventData {
  @Default private Map<String, String> properties = new HashMap<>();
  // TODO : Remove this value once prometheus is deprecated
  private double value;

  /**
   * Any model that you want to put in the queue should implement EventInfo
   * and on the handler side, you can cast it to your model
   */
  private EventInfo eventInfo;
}
