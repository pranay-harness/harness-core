package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;

import javax.validation.constraints.NotNull;

@Entity(value = "resourceLookup", noClassnameStored = true)
@HarnessExportableEntity
@FieldNameConstants(innerTypeName = "ResourceLookupKeys")
@Indexes({
  @Index(options = @IndexOptions(name = "resourceIndex_1"),
      fields =
      {
        @Field(ResourceLookupKeys.accountId)
        , @Field(ResourceLookupKeys.resourceType), @Field(ResourceLookupKeys.appId),
            @Field(ResourceLookupKeys.resourceName)
      })
  ,
      @Index(options = @IndexOptions(name = "resourceIndex_2"),
          fields =
          {
            @Field(ResourceLookupKeys.accountId)
            , @Field(ResourceLookupKeys.appId), @Field(ResourceLookupKeys.resourceType),
                @Field(ResourceLookupKeys.resourceName)
          }),

      @Index(options = @IndexOptions(name = "resourceIndex_3"), fields = {
        @Field(ResourceLookupKeys.accountId)
        , @Field(ResourceLookupKeys.resourceName), @Field(ResourceLookupKeys.resourceType)
      })
})
@Data
@Builder
public class ResourceLookup implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @Indexed @NotEmpty private String resourceId;
  @NotEmpty private String resourceType;
  private String resourceName;
  private long createdAt;
  private long lastUpdatedAt;
}
