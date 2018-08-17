package software.wings.beans.instance.dashboard;

/**
 * Service info with parent app info
 * @author rktummala on 08/13/17
 */
public class ServiceSummary extends AbstractEntitySummary {
  private EntitySummary appSummary;

  public EntitySummary getAppSummary() {
    return appSummary;
  }

  public void setAppSummary(EntitySummary appSummary) {
    this.appSummary = appSummary;
  }

  public static final class Builder {
    private String id;
    private String name;
    private String type;
    private EntitySummary appSummary;

    private Builder() {}

    public static Builder aServiceSummary() {
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

    public Builder appSummary(EntitySummary appSummary) {
      this.appSummary = appSummary;
      return this;
    }

    public ServiceSummary build() {
      ServiceSummary serviceSummary = new ServiceSummary();
      serviceSummary.setId(id);
      serviceSummary.setName(name);
      serviceSummary.setType(type);
      serviceSummary.setAppSummary(appSummary);
      return serviceSummary;
    }
  }
}
