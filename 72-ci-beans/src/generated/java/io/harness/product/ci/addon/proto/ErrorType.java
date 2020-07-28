// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/addon/proto/addon.proto

package io.harness.product.ci.addon.proto;

/**
 * Protobuf enum {@code io.harness.product.ci.addon.proto.ErrorType}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:ErrorType.java.pb.meta")
public enum ErrorType implements com
.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>NO_ERROR = 0;</code>
   */
  NO_ERROR(0),
      /**
       * <code>NETWORK_ERROR = 1;</code>
       */
      NETWORK_ERROR(1),
      /**
       * <code>AUTHENTICATION_ERROR = 2;</code>
       */
      AUTHENTICATION_ERROR(2), UNRECOGNIZED(-1),
      ;

  /**
   * <code>NO_ERROR = 0;</code>
   */
  public static final int NO_ERROR_VALUE = 0;
  /**
   * <code>NETWORK_ERROR = 1;</code>
   */
  public static final int NETWORK_ERROR_VALUE = 1;
  /**
   * <code>AUTHENTICATION_ERROR = 2;</code>
   */
  public static final int AUTHENTICATION_ERROR_VALUE = 2;

  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException("Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static ErrorType valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static ErrorType forNumber(int value) {
    switch (value) {
      case 0:
        return NO_ERROR;
      case 1:
        return NETWORK_ERROR;
      case 2:
        return AUTHENTICATION_ERROR;
      default:
        return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<ErrorType> internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<ErrorType> internalValueMap =
      new com.google.protobuf.Internal.EnumLiteMap<ErrorType>() {
        public ErrorType findValueByNumber(int number) {
          return ErrorType.forNumber(number);
        }
      };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
    return io.harness.product.ci.addon.proto.AddonOuterClass.getDescriptor().getEnumTypes().get(0);
  }

  private static final ErrorType[] VALUES = values();

  public static ErrorType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private ErrorType(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:io.harness.product.ci.addon.proto.ErrorType)
}
