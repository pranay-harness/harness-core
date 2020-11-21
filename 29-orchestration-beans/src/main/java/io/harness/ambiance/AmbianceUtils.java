package io.harness.ambiance;

import io.harness.serializer.KryoSerializer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import lombok.NonNull;

public class AmbianceUtils {
  @Inject private KryoSerializer kryoSerializer;

  public Ambiance cloneForFinish(@NonNull Ambiance ambiance) {
    return clone(ambiance, ambiance.getLevels().size() - 1);
  }

  public Ambiance cloneForChild(@NonNull Ambiance ambiance) {
    return clone(ambiance, ambiance.getLevels().size());
  }

  public Ambiance clone(Ambiance ambiance, int levelsToKeep) {
    Ambiance cloned = deepCopy(ambiance);
    if (levelsToKeep >= 0 && levelsToKeep < ambiance.getLevels().size()) {
      cloned.levels.subList(levelsToKeep, cloned.levels.size()).clear();
    }
    return cloned;
  }

  @VisibleForTesting
  Ambiance deepCopy(Ambiance ambiance) {
    return kryoSerializer.clone(ambiance);
  }
}
