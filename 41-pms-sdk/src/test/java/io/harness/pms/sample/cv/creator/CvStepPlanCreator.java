package io.harness.pms.sample.cv.creator;

import com.google.common.collect.Sets;

import io.harness.pms.sdk.creator.SimpleStepPlanCreator;

import java.util.Set;

public class CvStepPlanCreator extends SimpleStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("appdVerify");
  }
}
