/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * The Class ServiceElement.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@TargetModule(_957_CG_BEANS)
@OwnedBy(CDC)
public class ServiceElement implements ContextElement {
  private String uuid;
  private String name;
  private String description;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SERVICE;
  }

  @Override
  public ContextElement cloneMin() {
    return ServiceElement.builder().uuid(uuid).name(name).description(description).build();
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, this);
    return map;
  }
}
