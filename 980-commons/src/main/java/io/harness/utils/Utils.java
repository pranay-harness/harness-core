package io.harness.utils;

import java.util.List;
import javax.annotation.Nullable;

public class Utils {
  private Utils() {}

  @Nullable
  public static <T1, T2 extends T1> T2 getFirstInstance(List<T1> inputs, Class<T2> cls) {
    return (T2) inputs.stream().filter(cls::isInstance).findFirst().orElse(null);
  }
}
