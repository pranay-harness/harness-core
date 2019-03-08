package software.wings.beans;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.mongo.KryoConverter;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.converters.SimpleValueConverter;
import software.wings.beans.DelegateTask.ParametersConverter;
import software.wings.beans.DelegateTask.ResponseDataConverter;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(exclude = {"uuid", "createdAt", "lastUpdatedAt", "validUntil"})
@Entity(value = "delegateTasks", noClassnameStored = true)
@Converters({ParametersConverter.class, ResponseDataConverter.class})
public class DelegateTask implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static final String APP_ID_KEY = "appId";
  public static final String NOTIFY_RESPONSE_KEY = "notifyResponse";

  public static final long DEFAULT_SYNC_CALL_TIMEOUT = 60 * 1000; // 1 minute
  public static final long DEFAULT_ASYNC_CALL_TIMEOUT = 10 * 60 * 1000; // 10 minutes

  @Id private String uuid;
  @Indexed protected String appId;
  private long createdAt;
  private long lastUpdatedAt;

  private String version;
  @NotNull private String taskType;
  private Object[] parameters;
  private List<String> tags;
  @NotEmpty private String accountId;
  private String waitId;
  private Status status;
  private String delegateId;
  private long timeout;
  private boolean async;
  private String envId;
  private String infrastructureMappingId;
  private Long validationStartedAt;
  private Long lastBroadcastAt;
  private int broadcastCount;
  private Set<String> validatingDelegateIds;
  private Set<String> validationCompleteDelegateIds;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;
  private String serviceTemplateId;
  private String artifactStreamId;
  private String correlationId;
  private String workflowExecutionId;
  private ResponseData notifyResponse;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  @Default
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(2).toInstant());

  @Value
  @Builder
  public static class SyncTaskContext {
    private String accountId;
    private String appId;
    private String envId;
    private String infrastructureMappingId;
    private long timeout;
    private List<String> tags;
    private String correlationId;
  }

  public static class ParametersConverter extends KryoConverter {
    public ParametersConverter() {
      super(Object[].class);
    }
  }

  public static class ResponseDataConverter extends KryoConverter implements SimpleValueConverter {
    public ResponseDataConverter() {
      super(ResponseData.class);
    }
  }

  public enum Status { QUEUED, STARTED, FINISHED, ERROR, ABORTED }
}
