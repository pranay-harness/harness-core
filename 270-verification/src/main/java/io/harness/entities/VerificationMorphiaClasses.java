package io.harness.entities;

import software.wings.service.impl.analysis.SupervisedTrainingStatus;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class VerificationMorphiaClasses {
  // TODO: this is temporarily listing all the classes in the Verification Service.
  //       Step by step this should be split in different dedicated sections

  public static final Set<Class> classes = ImmutableSet.<Class>of(TimeSeriesAnomaliesRecord.class,
      TimeSeriesCumulativeSums.class, AnomalousLogRecord.class, SupervisedTrainingStatus.class);
}
