package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity(value = "triggers")
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Trigger extends Base {
  @NotEmpty private String name;
  private String description;
  @NotNull private TriggerCondition condition;
  @NotEmpty private String pipelineId;
  private String pipelineName;
  private List<ArtifactSelection> artifactSelections = new ArrayList<>();
  @JsonIgnore @Indexed private String webHookToken;

  public static final class Builder {
    protected String appId;
    private String uuid;
    private String name;
    private String description;
    private TriggerCondition condition;
    private String pipelineId;
    private List<ArtifactSelection> artifactSelections = new ArrayList<>();

    private Builder() {}

    public static Builder aTrigger() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withCondition(TriggerCondition condition) {
      this.condition = condition;
      return this;
    }

    public Builder withPipelineId(String pipelineId) {
      this.pipelineId = pipelineId;
      return this;
    }

    public Builder withArtifactSelections(List<ArtifactSelection> artifactSelections) {
      this.artifactSelections = artifactSelections;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Trigger build() {
      Trigger trigger = new Trigger();
      trigger.setName(name);
      trigger.setDescription(description);
      trigger.setCondition(condition);
      trigger.setPipelineId(pipelineId);
      trigger.setArtifactSelections(artifactSelections);
      trigger.setAppId(appId);
      trigger.setUuid(uuid);
      return trigger;
    }
  }
}
