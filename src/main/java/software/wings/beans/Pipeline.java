/**
 *
 */

package software.wings.beans;

import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;

/**
 * The Class Pipeline.
 *
 * @author Rishi
 */
@Entity(value = "pipelines", noClassnameStored = true)
public class Pipeline extends Workflow {
  private String cronSchedule;

  /**
   * Gets cron schedule.
   *
   * @return the cron schedule
   */
  public String getCronSchedule() {
    return cronSchedule;
  }

  /**
   * Sets cron schedule.
   *
   * @param cronSchedule the cron schedule
   */
  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }

  /**
   * The type Builder.
   */
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

    /**
     * A pipeline builder.
     *
     * @return the builder
     */
    public static Builder aPipeline() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With graph builder.
     *
     * @param graph the graph
     * @return the builder
     */
    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    /**
     * Add services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder addServices(String... services) {
      this.services.addAll(asList(services));
      return this;
    }

    /**
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<String> services) {
      this.services = services;
      return this;
    }

    /**
     * With cron schedule builder.
     *
     * @param cronSchedule the cron schedule
     * @return the builder
     */
    public Builder withCronSchedule(String cronSchedule) {
      this.cronSchedule = cronSchedule;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
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

    /**
     * Build pipeline.
     *
     * @return the pipeline
     */
    public Pipeline build() {
      Pipeline pipeline = new Pipeline();
      pipeline.setName(name);
      pipeline.setDescription(description);
      pipeline.setGraph(graph);
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
