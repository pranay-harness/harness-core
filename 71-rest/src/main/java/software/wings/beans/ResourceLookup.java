package software.wings.beans;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.Indexes;
import io.harness.persistence.AccountAccess;
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
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;

import java.util.List;
import javax.validation.constraints.NotNull;

@FieldNameConstants(innerTypeName = "ResourceLookupKeys")
@Indexes({
  @Index(name = "resourceIndex_1",
      fields =
      {
        @Field(ResourceLookupKeys.accountId)
        , @Field(ResourceLookupKeys.resourceType), @Field(ResourceLookupKeys.appId),
            @Field(ResourceLookupKeys.resourceName)
      })
  ,
      @Index(name = "resourceIndex_3",
          fields =
          {
            @Field(ResourceLookupKeys.accountId)
            , @Field(ResourceLookupKeys.resourceName), @Field(ResourceLookupKeys.resourceType)
          }),

      @Index(
          name = "tagsNameResourceLookupIndex", fields = { @Field(ResourceLookupKeys.accountId)
                                                           , @Field("tags.name") }),

      @Index(name = "resourceIdResourceLookupIndex", fields = {
        @Field(ResourceLookupKeys.accountId), @Field(ResourceLookupKeys.resourceId)
      }),
})
@Data
@Builder
@Entity(value = "resourceLookup", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ResourceLookup implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @Indexed @NotEmpty private String resourceId;
  @NotEmpty private String resourceType;
  private String resourceName;
  private List<NameValuePair> tags;
  @Indexed private long createdAt;
  private long lastUpdatedAt;
}
