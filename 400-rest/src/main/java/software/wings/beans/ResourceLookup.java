package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.beans.ResourceLookup.ResourceLookupKeys;

import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "ResourceLookupKeys")

@CdIndex(name = "resourceIndex_1",
    fields =
    {
      @Field(ResourceLookupKeys.accountId)
      , @Field(ResourceLookupKeys.resourceType), @Field(ResourceLookupKeys.appId),
          @Field(ResourceLookupKeys.resourceName)
    })
@CdIndex(name = "resourceIndex_3",
    fields =
    {
      @Field(ResourceLookupKeys.accountId)
      , @Field(ResourceLookupKeys.resourceName), @Field(ResourceLookupKeys.resourceType)
    })
@CdIndex(name = "tagsNameResourceLookupIndex", fields = { @Field(ResourceLookupKeys.accountId)
                                                          , @Field("tags.name") })
@CdIndex(name = "resourceIdResourceLookupIndex",
    fields = { @Field(ResourceLookupKeys.accountId)
               , @Field(ResourceLookupKeys.resourceId) })
@Data
@Builder
@Entity(value = "resourceLookup", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ResourceLookup implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @FdIndex @NotEmpty private String resourceId;
  @NotEmpty private String resourceType;
  private String resourceName;
  private List<NameValuePair> tags;
  @FdIndex private long createdAt;
  private long lastUpdatedAt;
}
