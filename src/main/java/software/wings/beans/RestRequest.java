package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class RestRequest.
 *
 * @param <T> the generic type
 */
public class RestRequest<T> {
  private Map<String, Object> metaData;

  private T resource;

  /**
   * Instantiates a new rest request.
   */
  public RestRequest() {
    this(null);
  }

  /**
   * Instantiates a new rest request.
   *
   * @param resource the resource
   */
  public RestRequest(T resource) {
    this.resource = resource;
    metaData = new HashMap<String, Object>();
  }

  public Map<String, Object> getMetaData() {
    return metaData;
  }

  public void setMetaData(Map<String, Object> metaData) {
    this.metaData = metaData;
  }

  public T getResource() {
    return resource;
  }

  public void setResource(T resource) {
    this.resource = resource;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("metaData", metaData).add("resource", resource).toString();
  }
}
