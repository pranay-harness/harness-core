package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 *
 * @author rktummala on 02/21/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EnvFilter extends Filter {
  public interface FilterType {
    String PROD = "PROD";
    String NON_PROD = "NON_PROD";
    String SELECTED = "SELECTED";
  }
  private Set<String> filterTypes;

  @Builder
  public EnvFilter(Set<String> ids, Set<String> filterTypes) {
    super(ids);
    this.filterTypes = filterTypes;
  }
}
