package software.wings.security;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
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

    static boolean isValidFilterType(String filterType) {
      switch (filterType) {
        case ALL:
        case SELECTED:
          return true;
        default:
          return false;
      }
    }
  }

  private String filterType;

  @Builder
  public GenericEntityFilter(Set<String> ids, String filterType) {
    super(ids);
    this.filterType = filterType;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends Filter.Yaml {
    private String filterType;

    @Builder
    public Yaml(List<String> names, String filterType) {
      super(names);
      this.filterType = filterType;
    }
  }
}
