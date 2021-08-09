package io.harness.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.ImmutableMap;
import java.lang.management.MemoryUsage;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DEL)
@UtilityClass

public class MemoryPerformanceUtils {
  public static void memoryUsage(ImmutableMap.Builder<String, String> builder, String prefix, MemoryUsage memoryUsage) {
    builder.put(prefix + "init", Long.toString(memoryUsage.getInit()));
    builder.put(prefix + "used", Long.toString(memoryUsage.getUsed()));
    builder.put(prefix + "committed", Long.toString(memoryUsage.getCommitted()));
    builder.put(prefix + "max", Long.toString(memoryUsage.getMax()));
  }
}
