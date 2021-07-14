package io.harness.core;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.BooleanRecastTransformer;
import io.harness.transformers.simplevalue.ByteRecastTransformer;
import io.harness.transformers.simplevalue.CharacterArrayRecastTransformer;
import io.harness.transformers.simplevalue.CharacterRecastTransformer;
import io.harness.transformers.simplevalue.ClassRecastTransformer;
import io.harness.transformers.simplevalue.DateRecastTransformer;
import io.harness.transformers.simplevalue.DoubleRecastTransformer;
import io.harness.transformers.simplevalue.EnumRecastTransformer;
import io.harness.transformers.simplevalue.FloatRecastTransformer;
import io.harness.transformers.simplevalue.InstantRecastTransformer;
import io.harness.transformers.simplevalue.IntegerRecastTransformer;
import io.harness.transformers.simplevalue.IterableRecastTransformer;
import io.harness.transformers.simplevalue.LocalDateRecastTransformer;
import io.harness.transformers.simplevalue.LocalDateTimeRecastTransformer;
import io.harness.transformers.simplevalue.LocalTimeRecastTransformer;
import io.harness.transformers.simplevalue.LongRecastTransformer;
import io.harness.transformers.simplevalue.MapRecastTransformer;
import io.harness.transformers.simplevalue.StringRecastTransformer;

public class DefaultTransformer extends Transformer {
  public DefaultTransformer(Recaster recaster) {
    super(recaster);
    initializeTransformers();
  }

  private void initializeTransformers() {
    addTransformer(new BooleanRecastTransformer());
    addTransformer(new ByteRecastTransformer());
    addTransformer(new ClassRecastTransformer());
    addTransformer(new EnumRecastTransformer());
    addTransformer(new StringRecastTransformer());
    addTransformer(new LongRecastTransformer());
    addTransformer(new IntegerRecastTransformer());
    addTransformer(new DoubleRecastTransformer());
    addTransformer(new FloatRecastTransformer());
    addTransformer(new CharacterRecastTransformer());
    addTransformer(new CharacterArrayRecastTransformer());
    addTransformer(new DateRecastTransformer());
    addTransformer(new LocalDateRecastTransformer());
    addTransformer(new LocalDateTimeRecastTransformer());
    addTransformer(new LocalTimeRecastTransformer());
    addTransformer(new InstantRecastTransformer());
    addTransformer(new MapRecastTransformer());
    addTransformer(new IterableRecastTransformer());
  }

  @Override
  protected RecastTransformer getTransformer(final Object val, final CastedField cf) {
    return super.getTransformer(val, cf);
  }
}
