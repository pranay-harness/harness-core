/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
