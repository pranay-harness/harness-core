package io.harness.pipeline.plan.scratch.cv;

import com.google.common.collect.Sets;

import io.harness.pipeline.plan.scratch.lib.creator.SimpleStepPlanCreator;

import java.util.Set;

public class CVStepPlanCreator extends SimpleStepPlanCreator {
  private static final Set<String> supportedStepTypes = Sets.newHashSet("appdVerify");

  @Override
  public Set<String> getSupportedStepTypes() {
    return supportedStepTypes;
  }
}
