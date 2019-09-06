package io.harness.exception;

import com.google.common.base.Joiner;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

public class EmptyRestResponseException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public EmptyRestResponseException(@NotEmpty String uri, String message) {
    super(null, null, ErrorCode.UNKNOWN_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_KEY,
        Joiner.on(StringUtils.SPACE)
            .join("Received empty rest response for"
                    + " call to",
                uri, message));
  }
}
