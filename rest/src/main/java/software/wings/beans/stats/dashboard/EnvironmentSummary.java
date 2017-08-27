package software.wings.beans.stats.dashboard;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
public class EnvironmentSummary extends EntitySummary {
  private boolean prod;

  public boolean isProd() {
    return prod;
  }

  public void setProd(boolean prod) {
    this.prod = prod;
  }

  public static final class Builder extends EntitySummary.Builder {
    private boolean prod;

    private Builder() {
      super();
    }

    public static Builder anEnvironmentSummary() {
      return new Builder();
    }

    public Builder withProd(boolean prod) {
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
