package software.wings.service.impl.appdynamics;

/**
 * Created by rsingh on 5/11/17.
 */
public class AppdynamicsBusinessTransaction {
  private long id;
  private String name;
  private String entryPointType;
  private String internalName;
  private long tierId;
  private String tierName;
  private boolean background;

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

  public String getEntryPointType() {
    return entryPointType;
  }

  public void setEntryPointType(String entryPointType) {
    this.entryPointType = entryPointType;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public long getTierId() {
    return tierId;
  }

  public void setTierId(long tierId) {
    this.tierId = tierId;
  }

  public String getTierName() {
    return tierName;
  }

  public void setTierName(String tierName) {
    this.tierName = tierName;
  }

  public boolean isBackground() {
    return background;
  }

  public void setBackground(boolean background) {
    this.background = background;
  }

  @Override
  public String toString() {
    return "AppdynamicsBusinessTransaction{"
        + "id=" + id + ", name='" + name + '\'' + ", entryPointType='" + entryPointType + '\'' + ", internalName='"
        + internalName + '\'' + ", tierId=" + tierId + ", tierName='" + tierName + '\'' + ", background=" + background
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    AppdynamicsBusinessTransaction that = (AppdynamicsBusinessTransaction) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }
}
