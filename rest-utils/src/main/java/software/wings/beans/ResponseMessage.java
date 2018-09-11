package software.wings.beans;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.Level.ERROR;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
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

  @Builder.Default private ErrorCode code = DEFAULT_ERROR_CODE;
  @Builder.Default private Level level = ERROR;

  private String message;
}
