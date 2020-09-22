package io.harness.cvng.core.entities;

public class CustomActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.CUSTOM;
  }

  @Override
  public void validateActivityParams() {}
}
