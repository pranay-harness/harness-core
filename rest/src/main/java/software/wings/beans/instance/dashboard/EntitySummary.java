package software.wings.beans.instance.dashboard;

/**
 * General construct that could be used anywhere
 * @author rktummala on 08/13/17
 */
public class EntitySummary extends AbstractEntitySummary {
  public static final class Builder {
    private String id;
    private String name;
    private String type;

    private Builder() {}

    public static Builder anEntitySummary() {
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

    public EntitySummary build() {
      EntitySummary entitySummary = new EntitySummary();
      entitySummary.setId(id);
      entitySummary.setName(name);
      entitySummary.setType(type);
      return entitySummary;
    }
  }
}
