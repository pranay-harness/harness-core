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

/**
 *
 * @author rktummala on 02/21/18
 */
@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnvFilter extends Filter {
  public interface FilterType {
    String PROD = "PROD";
    String NON_PROD = "NON_PROD";
    String SELECTED = "SELECTED";

    static boolean isValidFilterType(String filterType) {
      switch (filterType) {
        case PROD:
        case NON_PROD:
        case SELECTED:
          return true;
        default:
          return false;
      }
    }
  }
  private Set<String> filterTypes;

  @Builder
  public EnvFilter(Set<String> ids, Set<String> filterTypes) {
    super(ids);
    this.filterTypes = filterTypes;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends Filter.Yaml {
    private List<String> filterTypes;

    @Builder
    public Yaml(List<String> entityNames, List<String> filterTypes) {
      super(entityNames);
      this.filterTypes = filterTypes;
    }
  }
}
