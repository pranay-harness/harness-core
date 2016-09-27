/**
 *
 */

package software.wings.stencils;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The enum Stencil category.
 *
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StencilCategory {
  /**
   * Commons stencil category.
   */
  COMMONS, /**
            * Commands stencil category.
            */
  COMMANDS(1), /**
                * Controls stencil category.
                */
  CONTROLS(0), /**
                * Environments stencil category.
                */
  ENVIRONMENTS(1), /**
                    * Build stencil category.
                    */
  BUILD(0), /**
             * Scripts stencil category.
             */
  SCRIPTS, /**
            * Verifications stencil category.
            */
  VERIFICATIONS(2), /**
                     * Copy stencil category.
                     */
  COPY, /**
         * Others stencil category.
         */
  CLOUD(3), /**
             * Others stencil category.
             */
  OTHERS(10);

  private String displayName;

  // priorities could 0, 1, 2, 3 and so on.
  private Integer displayOrder = 3;

  StencilCategory() {
    this(null, 3);
  }

  StencilCategory(Integer displayOrder) {
    this(null, displayOrder);
  }

  StencilCategory(String displayName) {
    if (displayName == null) {
      this.displayName = UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
    }
  }

  StencilCategory(String displayName, Integer displayOrder) {
    if (displayName == null) {
      this.displayName = UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
    }
    this.displayOrder = displayOrder;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets display order.
   *
   * @return the display order
   */
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name();
  }
}
