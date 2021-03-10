package software.wings.beans;

public enum DashboardPreference {
  NG,
  CG;

  public static boolean isNGDashboard(DashboardPreference dashboardPreference) {
    return dashboardPreference.equals(DashboardPreference.NG);
  }

  public static boolean isCGDashboard(DashboardPreference dashboardPreference) {
    return dashboardPreference.equals(DashboardPreference.CG);
  }
}
