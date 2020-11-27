package io.harness.resolvers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@Redesign
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
