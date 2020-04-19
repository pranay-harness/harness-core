package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
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

import java.util.List;
import javax.validation.constraints.NotNull;

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
      @Index(options = @IndexOptions(name = "resourceIndex_3"),
          fields =
          {
            @Field(ResourceLookupKeys.accountId)
            , @Field(ResourceLookupKeys.resourceName), @Field(ResourceLookupKeys.resourceType)
          }),

      @Index(options = @IndexOptions(name = "tagsNameResourceLookupIndex"),
          fields = { @Field(ResourceLookupKeys.accountId)
                     , @Field("tags.name") }),

      @Index(options = @IndexOptions(name = "resourceIdResourceLookupIndex"), fields = {
        @Field(ResourceLookupKeys.accountId), @Field(ResourceLookupKeys.resourceId)
      }),
})
@Data
@Builder
@Entity(value = "resourceLookup", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ResourceLookup implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @Indexed @NotEmpty private String resourceId;
  @NotEmpty private String resourceType;
  private String resourceName;
  private List<NameValuePair> tags;
  private long createdAt;
  private long lastUpdatedAt;
}
