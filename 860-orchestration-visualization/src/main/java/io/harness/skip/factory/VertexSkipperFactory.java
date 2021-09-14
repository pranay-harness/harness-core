/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.skip.factory;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.skip.skipper.VertexSkipper;
import io.harness.skip.skipper.impl.NoOpSkipper;
import io.harness.skip.skipper.impl.SkipNodeSkipper;
import io.harness.skip.skipper.impl.SkipTreeSkipper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class VertexSkipperFactory {
  @Inject private SkipNodeSkipper skipNodeSkipper;
  @Inject private SkipTreeSkipper skipTreeSkipper;
  @Inject private NoOpSkipper noOpSkipper;

  public VertexSkipper obtainVertexSkipper(SkipType skipType) {
    if (SkipType.SKIP_NODE == skipType) {
      return skipNodeSkipper;
    } else if (SkipType.SKIP_TREE == skipType) {
      return skipTreeSkipper;
    } else if (SkipType.NOOP == skipType) {
      return noOpSkipper;
    } else {
      throw new UnsupportedOperationException(format("Unsupported skipper type : [%s]", skipType));
    }
  }
}
