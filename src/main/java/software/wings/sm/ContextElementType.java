package software.wings.sm;

/**
 * Describes what type of element is being repeated on.
 *
 * @author Rishi
 */
public enum ContextElementType {
  /**
   * Service context element type.
   */
  SERVICE, /**
            * Tag context element type.
            */
  TAG, /**
        * Host context element type.
        */
  HOST, /**
         * Instance context element type.
         */
  INSTANCE, /**
             * Standard context element type.
             */
  STANDARD, /**
             * Param context element type.
             */
  PARAM, /**
          * Other context element type.
          */
  OTHER;
}
