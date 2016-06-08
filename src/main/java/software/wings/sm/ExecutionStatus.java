package software.wings.sm;

import java.io.Serializable;

/**
 * Describes possible execution statuses for a state.
 *
 * @author Rishi
 */
public enum ExecutionStatus implements Serializable {
  /**
   * New execution status.
   */
  NEW, /**
        * Running execution status.
        */
  RUNNING, /**
            * Success execution status.
            */
  SUCCESS, /**
            * Aborted execution status.
            */
  ABORTED, /**
            * Failed execution status.
            */
  FAILED, /**
           * Error execution status.
           */
  ERROR;
}
