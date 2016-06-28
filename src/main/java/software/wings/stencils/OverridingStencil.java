package software.wings.stencils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by peeyushaggarwal on 6/28/16.
 */
public interface OverridingStencil<T> extends Stencil<T> {
  void setOverridingJsonSchema(JsonNode overridingJsonSchema);

  String getOverridingName();

  void setOverridingName(String overridingName);
}
