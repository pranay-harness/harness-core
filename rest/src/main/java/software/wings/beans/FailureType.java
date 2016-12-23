package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

/**
 * Created by rishi on 10/31/16.
 */
@JsonFormat(shape = Shape.OBJECT)
public enum FailureType {
  CONNECTIVITY("Connectivity Error"),
  AUTHENTICATION("Authentication Failure"),
  APPLICATION_ERROR("Application Error");

  private String display;

  FailureType(String display) {
    this.display = display;
  }

  public String getDisplay() {
    return display;
  }
}
