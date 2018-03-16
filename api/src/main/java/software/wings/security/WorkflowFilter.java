package software.wings.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 *
 * @author rktummala on 02/08/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WorkflowFilter extends EnvFilter {
  public interface FilterType extends EnvFilter.FilterType { String TEMPLATES = "TEMPLATES"; }

  public WorkflowFilter(Set<String> ids, Set<String> filterTypes) {
    super(ids, filterTypes);
  }
}
