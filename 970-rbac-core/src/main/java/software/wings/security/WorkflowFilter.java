/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowFilter extends EnvFilter {
  public interface FilterType extends EnvFilter.FilterType {
    String TEMPLATES = "TEMPLATES";
  }

  public WorkflowFilter(Set<String> ids, Set<String> filterTypes) {
    super(ids, filterTypes);
  }
}
