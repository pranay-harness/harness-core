package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity(value = "schema", noClassnameStored = true)
public class Schema extends Base {
  public static final String SCHEMA_ID = "schema";
  public static final String VERSION_KEY = "version";
  public static final String BACKGROUND_VERSION_KEY = "backgroundVersion";
  public static final String SEED_DATA_VERSION_KEY = "seedDataVersion";

  private int version;
  private int backgroundVersion;
  private int seedDataVersion;

  public static final class SchemaBuilder {
    private int version;
    private int backgroundVersion;
    private int seedDataVersion;
    private String uuid = SCHEMA_ID;

    private SchemaBuilder() {}

    public static SchemaBuilder aSchema() {
      return new SchemaBuilder();
    }

    public SchemaBuilder withVersion(int version) {
      this.version = version;
      return this;
    }

    public SchemaBuilder withBackgroundVersion(int backgroundVersion) {
      this.backgroundVersion = backgroundVersion;
      return this;
    }

    public SchemaBuilder withSeedDataVersion(int seedDataVersion) {
      this.seedDataVersion = seedDataVersion;
      return this;
    }

    public Schema build() {
      Schema schema = new Schema();
      schema.setVersion(version);
      schema.setBackgroundVersion(backgroundVersion);
      schema.setUuid(uuid);
      schema.setSeedDataVersion(seedDataVersion);
      return schema;
    }
  }
}
