/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 7/24/18.
 */
@Data
@Builder
public class VerificationNodeDataSetupResponse {
  private boolean providerReachable;
  private VerificationLoadResponse loadResponse;
  private Object dataForNode;
  private boolean isConfigurationCorrect;

  @Data
  @Builder
  public static class VerificationLoadResponse {
    private boolean isLoadPresent;
    private Object loadResponse;
    private Long totalHits;
    private Long totalHitsThreshold;
  }
}
