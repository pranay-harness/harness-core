package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class HarnessReflections {
  private static final Map<Set<String>, Reflections> reflectionsLoadingCache = new ConcurrentHashMap<>();

  public Reflections getReflections(Set<String> packages) {
    if (!reflectionsLoadingCache.containsKey(packages)) {
      reflectionsLoadingCache.put(packages, new Reflections(packages));
    }
    return reflectionsLoadingCache.get(packages);
  }
}
