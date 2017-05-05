package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Objects;

/**
 * The Class ResponseMessage.
 */
public class ResponseMessage implements Serializable {
  private static final long serialVersionUID = 7669895652860634550L;

  private ErrorCode code;
  private ResponseTypeEnum errorType;
  private String message;

  /**
   * Gets code.
   *
   * @return the code
   */
  public ErrorCode getCode() {
    return code;
  }

  /**
   * Sets code.
   *
   * @param code the code
   */
  public void setCode(ErrorCode code) {
    this.code = code;
  }

  /**
   * Gets error type.
   *
   * @return the error type
   */
  public ResponseTypeEnum getErrorType() {
    return errorType;
  }

  /**
   * Sets error type.
   *
   * @param errorType the error type
   */
  public void setErrorType(ResponseTypeEnum errorType) {
    this.errorType = errorType;
  }

  /**
   * Gets message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets message.
   *
   * @param message the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * {@inheritDoc}
   */ /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("code", code)
        .add("errorType", errorType)
        .add("message", message)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, errorType, message);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ResponseMessage other = (ResponseMessage) obj;
    return Objects.equals(this.code, other.code) && Objects.equals(this.errorType, other.errorType)
        && Objects.equals(this.message, other.message);
  }

  /**
   * The Enum ResponseTypeEnum.
   */
  public enum ResponseTypeEnum {
    /**
     * Info response type enum.
     */
    INFO, /**
           * Warn response type enum.
           */
    WARN, /**
           * Error response type enum.
           */
    ERROR
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ErrorCode code;
    private ResponseTypeEnum errorType;
    private String message;

    /**
     * Do not instantiate Builder.
     */
    private Builder() {}

    /**
     * A response message builder.
     *
     * @return the builder
     */
    public static Builder aResponseMessage() {
      return new Builder();
    }

    /**
     * With code builder.
     *
     * @param code the code
     * @return the builder
     */
    public Builder withCode(ErrorCode code) {
      this.code = code;
      return this;
    }

    /**
     * With error type builder.
     *
     * @param errorType the error type
     * @return the builder
     */
    public Builder withErrorType(ResponseTypeEnum errorType) {
      this.errorType = errorType;
      return this;
    }

    /**
     * With message builder.
     *
     * @param message the message
     * @return the builder
     */
    public Builder withMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aResponseMessage().withCode(code).withErrorType(errorType).withMessage(message);
    }

    /**
     * Build response message.
     *
     * @return the response message
     */
    public ResponseMessage build() {
      ResponseMessage responseMessage = new ResponseMessage();
      responseMessage.setCode(code);
      responseMessage.setErrorType(errorType);
      responseMessage.setMessage(message);
      return responseMessage;
    }
  }
}
