package io.harness.pms.sample.cv.creator;

import io.harness.pms.sdk.creator.SimpleStepPlanCreator;

import com.google.common.collect.Sets;
import java.util.Set;

public class CvStepPlanCreator extends SimpleStepPlanCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("appdVerify");
  }
}
