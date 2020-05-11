package software.wings.stencils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by peeyushaggarwal on 6/28/16.
 *
 * @param <T> the type parameter
 */
@OwnedBy(CDC)
public interface OverridingStencil<T> extends Stencil<T> {
  /**
   * Gets overriding json schema.
   *
   * @return the overriding json schema
   */
  @JsonIgnore JsonNode getOverridingJsonSchema();

  /**
   * Sets overriding json schema.
   *
   * @param overridingJsonSchema the overriding json schema
   */
  void setOverridingJsonSchema(JsonNode overridingJsonSchema);

  /**
   * Gets overriding name.
   *
   * @return the overriding name
   */
  @JsonIgnore String getOverridingName();

  /**
   * Sets overriding name.
   *
   * @param overridingName the overriding name
   */
  void setOverridingName(String overridingName);
}
