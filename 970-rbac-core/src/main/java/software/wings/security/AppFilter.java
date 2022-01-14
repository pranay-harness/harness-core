package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AppFilter extends Filter {
  public interface FilterType {
    String ALL = "ALL";
    String SELECTED = "SELECTED";
    String EXCLUDE_SELECTED = "EXCLUDE_SELECTED";

    static boolean isValidFilterType(String filterType) {
      switch (filterType) {
        case ALL:
        case SELECTED:
        case EXCLUDE_SELECTED:
          return true;
        default:
          return false;
      }
    }
  }

  private String filterType;

  @Builder
  public AppFilter(Set<String> ids, String filterType) {
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
