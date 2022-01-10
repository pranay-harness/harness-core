/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
