/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
