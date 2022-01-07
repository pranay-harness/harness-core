/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.InvocationCount;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 01/03/18
 */
@Data
@Builder
public class InstanceSummaryStatsByService {
  private long totalCount;
  private long prodCount;
  private long nonprodCount;
  private ServiceSummary serviceSummary;
  private InvocationCount invocationCount;
}
