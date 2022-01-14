package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineServiceUtilityModule extends AbstractModule {
  private static PipelineServiceUtilityModule instance;

  public static PipelineServiceUtilityModule getInstance() {
    if (instance == null) {
      instance = new PipelineServiceUtilityModule();
    }
    return instance;
  }

  protected void configure() {}
}
