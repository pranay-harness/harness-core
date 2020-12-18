package io.harness.pms.sdk.core.recast;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public abstract class RecastConverter {
  @Setter @Getter private Recaster recaster;
  @Getter private List<Class<?>> supportedTypes;

  public RecastConverter(List<Class<?>> supportedTypes) {
    this.supportedTypes = supportedTypes;
  }

  public RecastConverter() {}

  public abstract Object decode(Class<?> targetClass, Object fromObject, CastedField castedField);

  public Object encode(Object value) {
    return encode(value, null);
  }

  public abstract Object encode(Object value, CastedField castedField);

  final boolean canConvert(CastedField cf) {
    return isSupported(cf.getType(), cf);
  }

  final boolean canConvert(Class c) {
    return isSupported(c, null);
  }

  protected boolean isSupported(final Class<?> c, final CastedField cf) {
    return false;
  }
}
