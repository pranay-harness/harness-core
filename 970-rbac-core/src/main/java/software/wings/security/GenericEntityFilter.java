package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
}
