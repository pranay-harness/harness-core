package software.wings.beans;

import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ResponseMessage.Level.ERROR;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * The Class ResponseMessage.
 */
@Builder(builderMethodName = "aResponseMessage")
@Getter
@EqualsAndHashCode
@ToString
public class ResponseMessage implements Serializable {
  private static final long serialVersionUID = 7669895652860634550L;

  public enum Level { DEBUG, INFO, WARN, ERROR }

  @Builder.Default private ErrorCode code = DEFAULT_ERROR_CODE;
  @Builder.Default private Level level = ERROR;

  private String message;
}
