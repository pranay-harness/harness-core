package io.harness.mongo.index;

/**
 * Defines the type of the index to create for a field.
 */
public enum IndexType {
  ASC(1),
  DESC(-1),
  GEO2D("2d"),
  GEO2DSPHERE("2dsphere"),
  HASHED("hashed"),
  TEXT("text");

  private final Object type;

  IndexType(final Object type) {
    this.type = type;
  }

  public static IndexType fromValue(Object value) {
    if (value instanceof Double) {
      value = (int) ((Double) value).doubleValue();
    } else if (value instanceof Float) {
      value = (int) ((Float) value).floatValue();
    }

    for (IndexType indexType : values()) {
      if (indexType.type.equals(value)) {
        return indexType;
      }
    }
    throw new IllegalArgumentException("No enum value found for " + value);
  }

  public Object toIndexValue() {
    return type;
  }
}
