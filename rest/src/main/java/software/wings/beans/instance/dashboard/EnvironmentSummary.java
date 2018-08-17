package software.wings.beans.instance.dashboard;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
public class EnvironmentSummary extends AbstractEntitySummary {
  private boolean prod;

  public boolean isProd() {
    return prod;
  }

  public void setProd(boolean prod) {
    this.prod = prod;
  }

  public static final class Builder {
    private String id;
    private String name;
    private String type;
    private boolean prod;

    private Builder() {}

    public static Builder anEnvironmentSummary() {
      return new Builder();
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder prod(boolean prod) {
      this.prod = prod;
      return this;
    }

    public EnvironmentSummary build() {
      EnvironmentSummary environmentSummary = new EnvironmentSummary();
      environmentSummary.setId(id);
      environmentSummary.setName(name);
      environmentSummary.setType(type);
      environmentSummary.setProd(prod);
      return environmentSummary;
    }
  }
}
