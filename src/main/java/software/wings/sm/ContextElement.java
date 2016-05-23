package software.wings.sm;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface ContextElement extends Serializable {
  public ContextElementType getElementType();

  public String getName();

  public Map<String, Object> paramMap();
}
