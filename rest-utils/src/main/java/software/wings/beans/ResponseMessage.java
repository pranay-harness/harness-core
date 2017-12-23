package software.wings.beans;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * The Class ResponseMessage.
 */
@Builder
@Getter
public class ResponseMessage implements Serializable {
  private static final long serialVersionUID = 7669895652860634550L;

  private ErrorCode code;
  private ResponseTypeEnum errorType;
  private String message;

  /**
   * The Enum ResponseTypeEnum.
   */
  public enum ResponseTypeEnum {
    /**
     * Info response type enum.
     */
    INFO,
    /**
     * Warn response type enum.
     */
    WARN,
    /**
     * Error response type enum.
     */
    ERROR
  }
}
