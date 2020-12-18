package io.harness.pms.sdk.core.recast;

import io.harness.pms.sdk.core.recast.converters.BooleanRecastConverter;
import io.harness.pms.sdk.core.recast.converters.ByteRecastConverter;
import io.harness.pms.sdk.core.recast.converters.CharacterArrayRecastConverter;
import io.harness.pms.sdk.core.recast.converters.CharacterRecastConverter;
import io.harness.pms.sdk.core.recast.converters.ClassRecastConverter;
import io.harness.pms.sdk.core.recast.converters.DoubleRecastConverter;
import io.harness.pms.sdk.core.recast.converters.EnumRecastConverter;
import io.harness.pms.sdk.core.recast.converters.FloatRecastConverter;
import io.harness.pms.sdk.core.recast.converters.IntegerRecastConverter;
import io.harness.pms.sdk.core.recast.converters.IterableRecastConverter;
import io.harness.pms.sdk.core.recast.converters.LongRecastConverter;
import io.harness.pms.sdk.core.recast.converters.ProtoRecastConverter;
import io.harness.pms.sdk.core.recast.converters.StringRecastConverter;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class Transformer {
  Recaster recaster;
  Map<Class<?>, RecastConverter> converterMap = new HashMap<>();

  public Transformer(Recaster recaster) {
    this.recaster = recaster;
    initializeConverters();
  }

  private void initializeConverters() {
    addConverter(new BooleanRecastConverter());
    addConverter(new ByteRecastConverter());
    addConverter(new ClassRecastConverter());
    addConverter(new EnumRecastConverter());
    addConverter(new IterableRecastConverter());
    addConverter(new ProtoRecastConverter());
    addConverter(new StringRecastConverter());
    addConverter(new LongRecastConverter());
    addConverter(new IntegerRecastConverter());
    addConverter(new DoubleRecastConverter());
    addConverter(new FloatRecastConverter());
    addConverter(new CharacterRecastConverter());
    addConverter(new CharacterArrayRecastConverter());
  }

  private RecastConverter addConverter(RecastConverter recastConverter) {
    if (recastConverter.getSupportedTypes() != null) {
      for (final Class c : recastConverter.getSupportedTypes()) {
        addTypedConverter(c, recastConverter);
      }
    }
    recastConverter.setRecaster(recaster);

    return recastConverter;
  }

  private void addTypedConverter(final Class type, final RecastConverter rc) {
    if (converterMap.containsKey(type)) {
      log.error("Added duplicate converter for " + type + " ; " + converterMap.get(type));
      // TODO : Create a custom exception here
      throw new RuntimeException();
    } else {
      converterMap.put(type, rc);
    }
  }

  public Object decode(final Class c, final Object fromDBObject, final CastedField cf) {
    Class toDecode = c;
    if (toDecode == null) {
      toDecode = fromDBObject.getClass();
    }
    return getEncoder(toDecode).decode(toDecode, fromDBObject, cf);
  }

  public Object encode(final Object o) {
    if (o == null) {
      return null;
    }
    return encode(o.getClass(), o);
  }

  public void toDocument(final Object containingObject, final CastedField cf, final Document document) {
    final Object fieldValue = cf.getFieldValue(containingObject);
    final RecastConverter enc = getEncoder(fieldValue, cf);

    final Object encoded = enc.encode(fieldValue, cf);
    document.put(cf.getNameToStore(), encoded);
  }

  public Object encode(final Class c, final Object o) {
    return getEncoder(c).encode(o, null);
  }

  protected RecastConverter getEncoder(final Class c) {
    RecastConverter recastConverter = converterMap.get(c);
    if (recastConverter != null) {
      return recastConverter;
    }

    for (RecastConverter rc : converterMap.values()) {
      if (rc.canConvert(c)) {
        return rc;
      }
    }

    return null;
  }

  protected RecastConverter getEncoder(final Object val, final CastedField cf) {
    RecastConverter rc = null;
    if (val != null) {
      rc = converterMap.get(val.getClass());
    }

    if (rc == null) {
      rc = converterMap.get(cf.getType());
    }

    if (rc != null) {
      return rc;
    }

    for (RecastConverter recastConverter : converterMap.values()) {
      if (recastConverter.canConvert(cf) && (val != null && recastConverter.isSupported(val.getClass(), cf))) {
        return recastConverter;
      }
    }

    return null;
  }

  public boolean hasConverter(CastedField cf) {
    return getEncoder(cf.getType()) != null;
  }

  public boolean hasConverter(Class c) {
    return getEncoder(c) != null;
  }
}
