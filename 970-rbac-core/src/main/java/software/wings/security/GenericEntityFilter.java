/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
