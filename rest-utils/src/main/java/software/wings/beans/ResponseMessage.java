package software.wings.beans;

import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ResponseMessage.Acuteness.SERIOUS;
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

  public enum Acuteness {
    /*
     * Serious acuteness indicates an issue report that requires harness side system admin / developer attention.
     * This is the default acuteness and it should be used for all issues that do not fit in any of the other
     * categories.
     */
    SERIOUS,

    /*
     * Alerting acuteness indicates an issue report that should be propagate as an alert to the customer alert
     * system for their admin to take a look. For example connectivity issues with services that are on their
     * side.
     */
    // TODO: this is just an idea for potential improvement, not implemented yet.
    //       for the time being it will act as HARMLESS.
    ALERTING,

    /*
     * Harmless acuteness indicates an issue report that is based on user feedback that is still a part of the normal
     * flow of the system. For example already used name for an entity, or deleting entity while still in use from
     * another. Such type of reports make sense to be reported to the user, but they do not indicate any problem with
     * the software or the system to require any further attention.
     */
    HARMLESS,

    /*
     * Ignorable acuteness indicates an issue report that is based on know technical context that does not require
     * fixing or attention. For example the system throw exception to terminate an execution of maintenance job
     * when persistent lock cannot be acquired (assuming - some other instance of the service is dealing with the
     * problem at the moment).
     */
    IGNORABLE,
  }

  @Builder.Default private ErrorCode code = DEFAULT_ERROR_CODE;
  @Builder.Default private Level level = ERROR;
  @Builder.Default private Acuteness acuteness = SERIOUS;
  private String message;
}
