package io.harness.pms.sdk.core.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.StepTransput;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import org.bson.Document;

@OwnedBy(CDC)
public interface Resolver<T extends StepTransput> {
  T resolve(@NotNull Ambiance ambiance, @NotNull RefObject refObject);

  default String consumeInternal(@NotNull Ambiance ambiance, @NotNull String name, T value, int levelsToKeep) {
    throw new UnsupportedOperationException("Method not supported for class : " + this.getClass().getName());
  };

  default String consume(@NotNull Ambiance ambiance, @NotNull String name, T value, String groupName) {
    if (hasNone(groupName)) {
      return consumeInternal(ambiance, name, value, -1);
    }
    if (groupName.equals(ResolverUtils.GLOBAL_GROUP_SCOPE)) {
      return consumeInternal(ambiance, name, value, 0);
    }

    if (hasNone(ambiance.getLevelsList())) {
      throw new GroupNotFoundException(groupName);
    }

    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (groupName.equals(level.getGroup())) {
        return consumeInternal(ambiance, name, value, i + 1);
      }
    }

    throw new GroupNotFoundException(groupName);
  }

  default Document convertToDocument(T value) {
    return RecastOrchestrationUtils.toDocument(value);
  }

  @SneakyThrows
  default T convertToObject(Document value, Class<T> clazz) {
    return RecastOrchestrationUtils.fromDocument(value, clazz);
  }
}
