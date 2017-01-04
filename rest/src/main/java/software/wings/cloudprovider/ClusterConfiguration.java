package software.wings.cloudprovider;

/**
 * Created by anubhaw on 12/29/16.
 */
public class ClusterConfiguration {
  private Integer size;
  private String name;

  /**
   * Gets size.
   *
   * @return the size
   */
  public Integer getSize() {
    return size;
  }

  /**
   * Sets size.
   *
   * @param size the size
   */
  public void setSize(Integer size) {
    this.size = size;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }
}
