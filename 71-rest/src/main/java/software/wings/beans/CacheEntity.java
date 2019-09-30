package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Date;

@Entity(value = "cache", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(fields = {
    @Field("_id"), @Field("contextValue")
  }, options = @IndexOptions(unique = true, name = "commutativeIdx"))
})
@Value
@Builder
@FieldNameConstants(innerTypeName = "CacheEntityKeys")
public class CacheEntity {
  private long contextValue;
  @Id private String canonicalKey;

  private byte[] entity;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
}
