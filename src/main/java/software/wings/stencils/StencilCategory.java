/**
 *
 */
package software.wings.stencils;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @author Rishi
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StencilCategory {
  COMMONS,
  COMMANDS,
  CONTROLS,
  ENVIRONMENTS,
  BUILD(null, 0),
  SCRIPTS,
  VERIFICATIONS,
  OTHERS;

  private String displayName;

  // priorities could 0, 1, 2, 3 and so on.
  private Integer displayOrder = 3;

  StencilCategory() {
    this(null);
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

  public String getDisplayName() {
    return displayName;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public String getName() {
    return name();
  }
}
