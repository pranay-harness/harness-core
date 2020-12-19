package io.harness.pms.sdk.core.recast;

import io.harness.pms.sdk.core.recast.beans.CastedField;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public abstract class RecastTransformer {
  @Setter @Getter private Recaster recaster;
  @Getter private List<Class<?>> supportedTypes;

  public RecastTransformer(List<Class<?>> supportedTypes) {
    this.supportedTypes = supportedTypes;
  }

  public RecastTransformer() {}

  public abstract Object decode(Class<?> targetClass, Object fromObject, CastedField castedField);

  public Object encode(Object value) {
    return encode(value, null);
  }

  public abstract Object encode(Object value, CastedField castedField);

  final boolean canTransform(CastedField cf) {
    return isSupported(cf.getType(), cf);
  }

  final boolean canTransform(Class c) {
    return isSupported(c, null);
  }

  protected boolean isSupported(final Class<?> c, final CastedField cf) {
    return false;
  }
}
