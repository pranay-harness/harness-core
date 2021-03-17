package software.wings.beans;

public enum DefaultExperience {
  NG,
  CG;

  public static boolean isNGExperience(DefaultExperience defaultExperience) {
    return defaultExperience.equals(DefaultExperience.NG);
  }

  public static boolean isCGExperience(DefaultExperience defaultExperience) {
    return defaultExperience.equals(DefaultExperience.CG);
  }
}
