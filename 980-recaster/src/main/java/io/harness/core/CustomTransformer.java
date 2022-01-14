package io.harness.core;

import io.harness.beans.CastedField;
import io.harness.transformers.DefaultRecastTransformer;
import io.harness.transformers.RecastTransformer;

public class CustomTransformer extends Transformer {
  private final RecastTransformer passThroughTransformer = new DefaultRecastTransformer();
  private final DefaultTransformer defaultTransformer;

  public CustomTransformer(Recaster recaster) {
    super(recaster);
    this.defaultTransformer = new DefaultTransformer(recaster);
  }

  @Override
  protected RecastTransformer getTransformer(final Class c) {
    RecastTransformer encoder = super.getTransformer(c);
    if (encoder == null) {
      encoder = defaultTransformer.getTransformer(c);
    }

    if (encoder != null) {
      return encoder;
    }

    return passThroughTransformer;
  }

  @Override
  protected RecastTransformer getTransformer(final Object val, final CastedField cf) {
    RecastTransformer encoder = super.getTransformer(val, cf);
    if (encoder == null) {
      encoder = defaultTransformer.getTransformer(val, cf);
    }

    if (encoder != null) {
      return encoder;
    }

    return passThroughTransformer;
  }
}
