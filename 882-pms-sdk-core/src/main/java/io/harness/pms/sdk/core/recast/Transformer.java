package io.harness.pms.sdk.core.recast;

import static java.lang.String.format;

import io.harness.pms.sdk.core.recast.beans.CastedField;
import io.harness.pms.sdk.core.recast.transformers.DefaultRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.IterableRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.ProtoRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.BooleanRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.ByteRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.CharacterArrayRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.CharacterRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.ClassRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.DoubleRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.EnumRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.FloatRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.IntegerRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.LongRecastTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.SimpleValueTransformer;
import io.harness.pms.sdk.core.recast.transformers.simplevalue.StringRecastTransformer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class Transformer {
  Recaster recaster;
  Map<Class<?>, RecastTransformer> converterMap = new HashMap<>();
  private final List<RecastTransformer> untypedTypeTransformers = new LinkedList<>();
  private final RecastTransformer passThroughTransformer = new DefaultRecastTransformer();

  public Transformer(Recaster recaster) {
    this.recaster = recaster;
    initializeTransformers();
  }

  private void initializeTransformers() {
    addTransformer(new BooleanRecastTransformer());
    addTransformer(new ByteRecastTransformer());
    addTransformer(new ClassRecastTransformer());
    addTransformer(new EnumRecastTransformer());
    addTransformer(new IterableRecastTransformer());
    addTransformer(new ProtoRecastTransformer());
    addTransformer(new StringRecastTransformer());
    addTransformer(new LongRecastTransformer());
    addTransformer(new IntegerRecastTransformer());
    addTransformer(new DoubleRecastTransformer());
    addTransformer(new FloatRecastTransformer());
    addTransformer(new CharacterRecastTransformer());
    addTransformer(new CharacterArrayRecastTransformer());
  }

  private RecastTransformer addTransformer(RecastTransformer recastTransformer) {
    if (recastTransformer.getSupportedTypes() != null) {
      for (final Class c : recastTransformer.getSupportedTypes()) {
        addTypedConverter(c, recastTransformer);
      }
    } else {
      untypedTypeTransformers.add(recastTransformer);
    }
    recastTransformer.setRecaster(recaster);

    return recastTransformer;
  }

  private void addTypedConverter(final Class type, final RecastTransformer rc) {
    if (converterMap.containsKey(type)) {
      log.error("Added duplicate converter for " + type + " ; " + converterMap.get(type));
      // TODO : Create a custom exception here
      throw new RuntimeException();
    } else {
      converterMap.put(type, rc);
    }
  }

  public Object decode(final Class c, final Object docObject, final CastedField cf) {
    Class toDecode = c;
    if (toDecode == null) {
      toDecode = docObject.getClass();
    }
    return getTransformer(toDecode).decode(toDecode, docObject, cf);
  }

  public Object encode(final Object o) {
    if (o == null) {
      return null;
    }
    return encode(o.getClass(), o);
  }

  public void fromDocument(final Object targetEntity, final CastedField cf, final Document document) {
    final Object object = cf.getDocumentValue(document);
    if (object != null) {
      RecastTransformer transformer = getTransformer(null, cf);
      Object decodedValue = transformer.decode(cf.getType(), object, cf);
      try {
        cf.setFieldValue(targetEntity, decodedValue);
      } catch (IllegalArgumentException e) {
        throw new RecasterException(format("Error setting value from converter (%s) for %s to %s",
                                        transformer.getClass().getSimpleName(), cf.getFullName(), decodedValue),
            e);
      }
    }
  }

  public void toDocument(final Object containingObject, final CastedField cf, final Document document) {
    final Object fieldValue = cf.getFieldValue(containingObject);
    final RecastTransformer enc = getTransformer(fieldValue, cf);

    final Object encoded = enc.encode(fieldValue, cf);
    document.put(cf.getNameToStore(), encoded);
  }

  public Object encode(final Class c, final Object o) {
    return getTransformer(c).encode(o, null);
  }

  protected RecastTransformer getTransformer(final Class c) {
    RecastTransformer recastTransformer = converterMap.get(c);
    if (recastTransformer != null) {
      return recastTransformer;
    }

    for (RecastTransformer rc : untypedTypeTransformers) {
      if (rc.canTransform(c)) {
        return rc;
      }
    }

    // TODO(Alexei) Throw exception
    return null;
  }

  protected RecastTransformer getTransformer(final Object val, final CastedField cf) {
    RecastTransformer rc = null;
    if (val != null) {
      rc = converterMap.get(val.getClass());
    }

    if (rc == null) {
      rc = converterMap.get(cf.getType());
    }

    if (rc != null) {
      return rc;
    }

    for (RecastTransformer recastTransformer : untypedTypeTransformers) {
      if (recastTransformer.canTransform(cf) && (val != null && recastTransformer.isSupported(val.getClass(), cf))) {
        return recastTransformer;
      }
    }

    // TODO(Alexei) Throw exception
    return null;
  }

  public boolean hasSimpleValueTransformer(final Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof Class) {
      return hasSimpleValueTransformer((Class) o);
    } else if (o instanceof CastedField) {
      return hasSimpleValueTransformer((CastedField) o);
    } else {
      return hasSimpleValueTransformer(o.getClass());
    }
  }

  public boolean hasSimpleValueTransformer(CastedField cf) {
    return (getTransformer(cf) instanceof SimpleValueTransformer);
  }

  public boolean hasSimpleValueTransformer(Class c) {
    return (getTransformer(c) instanceof SimpleValueTransformer);
  }

  private RecastTransformer getTransformer(CastedField cf) {
    return getTransformer(null, cf);
  }
}
