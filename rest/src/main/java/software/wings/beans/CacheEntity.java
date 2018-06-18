package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.cache.Distributable;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity(value = "cache", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("contextHash"), @Field("algorithmId"), @Field("structureHash"), @Field("key")
  }, options = @IndexOptions(unique = true, name = "canonicalKeyIdx"))
})
@Value
@Builder
public class CacheEntity {
  public static final String CONTEXT_HASH_KEY = "contextHash";
  public static final String ALGORITHM_ID_KEY = "algorithmId";
  public static final String STRUCTURE_HASH_KEY = "structureHash";
  public static final String KEY_KEY = "key";

  private long contextHash;
  private long algorithmId;
  private long structureHash;
  @Id private String key;

  private Distributable entity;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(3).toInstant());
}
