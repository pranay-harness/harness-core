package io.harness.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowFilter extends EnvFilter {
  public interface FilterType extends EnvFilter.FilterType { String TEMPLATES = "TEMPLATES"; }

  public WorkflowFilter(Set<String> ids, Set<String> filterTypes) {
    super(ids, filterTypes);
  }
}
