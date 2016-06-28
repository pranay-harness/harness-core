package software.wings.stencils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface Stencil<T> {
  /**
   * Gets type.
   *
   * @return the type
   */
  String getType();

  /**
   * Gets state class.
   *
   * @return the state class
   */
  @JsonIgnore Class<?> getTypeClass();

  /**
   * Gets json schema.
   *
   * @return the json schema
   */
  JsonNode getJsonSchema();

  /**
   * Gets ui schema.
   *
   * @return the ui schema
   */
  Object getUiSchema();

  /**
   * Gets name.
   *
   * @return the name
   */
  String getName();

  @JsonIgnore OverridingStencil getOverridingStencil();

  /**
   * New instance.
   *
   * @param id the id
   * @return the state
   */
  @JsonIgnore T newInstance(String id);
}
