package software.wings.service.impl.appdynamics;

/**
 * Created by rsingh on 5/12/17.
 */
public class AppdynamicsTier {
  private long id;
  private String name;
  private String description;
  private String type;
  private String agentType;
  private int numberOfNodes;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAgentType() {
    return agentType;
  }

  public void setAgentType(String agentType) {
    this.agentType = agentType;
  }

  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  public void setNumberOfNodes(int numberOfNodes) {
    this.numberOfNodes = numberOfNodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AppdynamicsTier that = (AppdynamicsTier) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return "AppdynamicsTier{"
        + "id=" + id + ", name='" + name + '\'' + ", description='" + description + '\'' + ", type='" + type + '\''
        + ", agentType='" + agentType + '\'' + ", numberOfNodes=" + numberOfNodes + '}';
  }
}
