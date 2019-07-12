package software.wings.beans.ecs;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentIterable;
import io.harness.persistence.UpdatedAtAccess;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;

import java.time.Instant;
import javax.validation.constraints.NotNull;

@Entity(value = "ecsPollingJobEntity")
@FieldNameConstants(innerTypeName = "ECSPollingJobEntityKeys")
@HarnessExportableEntity
@Getter
@ToString
@EqualsAndHashCode
public class ECSPollingJobEntity implements PersistentIterable, CreatedAtAccess, UpdatedAtAccess {
  @Id private String uuid;
  @Indexed private String clusterName;
  private String serviceName;
  private String region;
  private String settingId;
  private AwsEcsRequestType awsEcsRequestType;
  @Setter @Indexed private Long nextIteration;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long createdAt;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore @NotNull private long lastUpdatedAt;

  public ECSPollingJobEntity(String clusterName, String serviceName, String region, String settingId,
      AwsEcsRequestType awsEcsRequestType, Long nextIteration) {
    long currentMillis = Instant.now().toEpochMilli();
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.settingId = settingId;
    this.region = region;
    this.awsEcsRequestType = awsEcsRequestType;
    this.nextIteration = nextIteration;
    this.createdAt = currentMillis;
    this.lastUpdatedAt = currentMillis;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public String getUuid() {
    return uuid;
  }
}
