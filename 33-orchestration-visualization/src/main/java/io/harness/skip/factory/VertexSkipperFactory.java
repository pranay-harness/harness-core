package io.harness.skip.factory;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.skip.SkipType;
import io.harness.skip.skipper.VertexSkipper;
import io.harness.skip.skipper.impl.NoOpSkipper;
import io.harness.skip.skipper.impl.SkipNodeSkipper;
import io.harness.skip.skipper.impl.SkipTreeSkipper;

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
