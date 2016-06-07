/**
 *
 */

package software.wings.beans;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * The Class Pipeline.
 *
 * @author Rishi
 */
@Entity(value = "pipelines", noClassnameStored = true)
public class Pipeline extends Workflow {
  @NotEmpty private List<String> services = Lists.newArrayList();

  private String cronSchedule;

  public List<String> getServices() {
    return services;
  }

  public void setServices(List<String> services) {
    this.services = services;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }

  public static final class Builder {
    private String name;
    private String description;
    private Graph graph;
    private List<String> services = Lists.newArrayList();
    private String cronSchedule;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aPipeline() {
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

    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public Builder addServices(String... services) {
      this.services.addAll(asList(services));
      return this;
    }

    public Builder withServices(List<String> services) {
      this.services = services;
      return this;
    }

    public Builder withCronSchedule(String cronSchedule) {
      this.cronSchedule = cronSchedule;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return aPipeline()
          .withName(name)
          .withDescription(description)
          .withGraph(graph)
          .withServices(services)
          .withCronSchedule(cronSchedule)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Pipeline build() {
      Pipeline pipeline = new Pipeline();
      pipeline.setName(name);
      pipeline.setDescription(description);
      pipeline.setGraph(graph);
      pipeline.setServices(services);
      pipeline.setCronSchedule(cronSchedule);
      pipeline.setUuid(uuid);
      pipeline.setAppId(appId);
      pipeline.setCreatedBy(createdBy);
      pipeline.setCreatedAt(createdAt);
      pipeline.setLastUpdatedBy(lastUpdatedBy);
      pipeline.setLastUpdatedAt(lastUpdatedAt);
      pipeline.setActive(active);
      return pipeline;
    }
  }
}
