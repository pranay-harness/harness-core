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
