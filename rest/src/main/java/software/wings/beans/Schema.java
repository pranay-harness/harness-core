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

  private int version;

  public static final class SchemaBuilder {
    private int version;
    private String uuid = SCHEMA_ID;

    private SchemaBuilder() {}

    public static SchemaBuilder aSchema() {
      return new SchemaBuilder();
    }

    public SchemaBuilder withVersion(int version) {
      this.version = version;
      return this;
    }

    public Schema build() {
      Schema schema = new Schema();
      schema.setVersion(version);
      schema.setUuid(uuid);
      return schema;
    }
  }
}
