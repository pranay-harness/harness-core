package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Date;

@Entity(value = "cache", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("_id"), @Field("contextValue")
  }, options = @IndexOptions(unique = true, name = "commutativeIdx"))
})
@Value
@Builder
public class CacheEntity {
  public static final String CONTEXT_VALUE_KEY = "contextValue";
  public static final String CANONICAL_KEY_KEY = "_id";
  public static final String ENTITY_KEY = "entity";
  public static final String VALID_UNTIL_KEY = "validUntil";

  private long contextValue;
  @Id private String canonicalKey;

  private byte[] entity;

  @SchemaIgnore @JsonIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
}
