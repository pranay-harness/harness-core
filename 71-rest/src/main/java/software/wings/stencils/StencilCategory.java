/**
 *
 */

package software.wings.stencils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.CaseFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;

/**
 * The enum Stencil category.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StencilCategory {
  BUILD(0),
  CLOUD(3),
  COLLABORATION("Collaboration", 4),
  COLLECTIONS,
  COMMANDS(1),
  COMMONS,
  CONFIGURATIONS,
  CONTAINERS,
  CONTROLS(0, true),
  COPY,
  ENVIRONMENTS(1, true),
  FLOW_CONTROLS("Flow controls", 9),
  KUBERNETES("Kubernetes", 0),
  OTHERS(10),
  PROVISIONERS(0),
  SCRIPTS,
  SUB_WORKFLOW(100, true),
  VERIFICATIONS(2),
  ECS("Ecs", 0),
  SPOTINST("Spotinst", 0),
  STAGING_ORIGINAL_EXECUTION(101, true);

  @Getter boolean hidden;

  private String displayName;

  // priorities could 0, 1, 2, 3 and so on.
  private Integer displayOrder = 3;

  StencilCategory() {
    this(null, 3);
  }

  StencilCategory(Integer displayOrder) {
    this(null, displayOrder);
  }

  StencilCategory(Integer displayOrder, boolean hidden) {
    this(null, displayOrder);
    this.hidden = hidden;
  }

  StencilCategory(String displayName) {
    if (displayName == null) {
      this.displayName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    }
  }

  StencilCategory(String displayName, Integer displayOrder) {
    if (displayName == null) {
      this.displayName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    } else {
      this.displayName = displayName;
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
