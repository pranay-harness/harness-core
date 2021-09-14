/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.model.marketo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class Campaign {
  private Input input;

  @Data
  @Builder
  public static class Input {
    private List<Id> leads;
  }
}
