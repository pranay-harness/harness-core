package software.wings.beans.stats.dashboard;

/**
 * General construct that could be used anywhere
 * @author rktummala on 08/13/17
 */
public class EntitySummary {
  private String id;
  private String name;
  private String type;

  public String getId() {
    return id;
  }

  protected void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  protected void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  protected void setType(String type) {
    this.type = type;
  }

  public static class Builder {
    protected String id;
    protected String name;
    protected String type;

    protected Builder() {}

    public static Builder anEntitySummary() {
      return new Builder();
    }

    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder but() {
      return anEntitySummary().withId(id).withName(name).withType(type);
    }

    public EntitySummary build() {
      EntitySummary entitySummary = new EntitySummary();
      entitySummary.setId(id);
      entitySummary.setName(name);
      entitySummary.setType(type);
      return entitySummary;
    }
  }
}
