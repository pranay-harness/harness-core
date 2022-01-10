/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.context;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("MdcGlobalContextData")
public class MdcGlobalContextData implements GlobalContextData {
  public static final String MDC_ID = "MDC";

  private Map<String, String> map;

  @Override
  public String getKey() {
    return MDC_ID;
  }
}
