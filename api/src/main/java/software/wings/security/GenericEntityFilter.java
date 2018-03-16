package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author rktummala on 02/08/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GenericEntityFilter extends Filter {
  public interface FilterType {
    String ALL = "ALL";
    String SELECTED = "SELECTED";
  }
  private String filterType;

  @Builder
  public GenericEntityFilter(Set<String> ids, String filterType) {
    super(ids);
    this.filterType = filterType;
  }
}
