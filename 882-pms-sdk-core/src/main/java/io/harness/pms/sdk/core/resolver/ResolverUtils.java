/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class ResolverUtils {
  public final String GLOBAL_GROUP_SCOPE = "__GLOBAL_GROUP_SCOPE__";

  public String prepareLevelRuntimeIdIdx(List<Level> levels) {
    return EmptyPredicate.isEmpty(levels) ? ""
                                          : levels.stream().map(Level::getRuntimeId).collect(Collectors.joining("|"));
  }

  public List<String> prepareLevelRuntimeIdIndices(@NotNull Ambiance ambiance) {
    if (EmptyPredicate.isEmpty(ambiance.getLevelsList())) {
      // If the ambiance has no levels, the instance also shouldn't have any levels.
      return Collections.singletonList("");
    }

    List<String> levelRuntimeIdIndices = new ArrayList<>();
    levelRuntimeIdIndices.add("");
    for (int i = 1; i <= ambiance.getLevelsList().size(); i++) {
      levelRuntimeIdIndices.add(prepareLevelRuntimeIdIdx(ambiance.getLevelsList().subList(0, i)));
    }
    return levelRuntimeIdIndices;
  }
}
