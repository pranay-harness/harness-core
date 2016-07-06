package software.wings.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 6/29/16.
 */
public class Setup {
  private Base entity;
  private SetupStatus setupStatus;
  private List<SetupAction> actions = new ArrayList<>();

  /**
   * Gets entity.
   *
   * @return the entity
   */
  public Base getEntity() {
    return entity;
  }

  /**
   * Sets entity.
   *
   * @param entity the entity
   */
  public void setEntity(Base entity) {
    this.entity = entity;
  }

  /**
   * Gets setup status.
   *
   * @return the setup status
   */
  public SetupStatus getSetupStatus() {
    return setupStatus;
  }

  /**
   * Sets setup status.
   *
   * @param setupStatus the setup status
   */
  public void setSetupStatus(SetupStatus setupStatus) {
    this.setupStatus = setupStatus;
  }

  /**
   * Gets actions.
   *
   * @return the actions
   */
  public List<SetupAction> getActions() {
    return actions;
  }

  /**
   * Sets actions.
   *
   * @param actions the actions
   */
  public void setActions(List<SetupAction> actions) {
    this.actions = actions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, setupStatus, actions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Setup other = (Setup) obj;
    return Objects.equals(this.entity, other.entity) && Objects.equals(this.setupStatus, other.setupStatus)
        && Objects.equals(this.actions, other.actions);
  }

  /**
   * The enum Setup status.
   */
  public enum SetupStatus {
    /**
     * Complete setup status.
     */
    COMPLETE, /**
               * Incomplete setup status.
               */
    INCOMPLETE
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Base entity;
    private SetupStatus setupStatus;
    private List<SetupAction> actions = new ArrayList<>();

    private Builder() {}

    /**
     * A setup builder.
     *
     * @return the builder
     */
    public static Builder aSetup() {
      return new Builder();
    }

    /**
     * With entity builder.
     *
     * @param entity the entity
     * @return the builder
     */
    public Builder withEntity(Base entity) {
      this.entity = entity;
      return this;
    }

    /**
     * With setup status builder.
     *
     * @param setupStatus the setup status
     * @return the builder
     */
    public Builder withSetupStatus(SetupStatus setupStatus) {
      this.setupStatus = setupStatus;
      return this;
    }

    /**
     * Add actions builder.
     *
     * @param actions the actions
     * @return the builder
     */
    public Builder addActions(SetupAction... actions) {
      Collections.addAll(this.actions, actions);
      return this;
    }

    /**
     * With actions builder.
     *
     * @param actions the actions
     * @return the builder
     */
    public Builder withActions(List<SetupAction> actions) {
      this.actions = actions;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSetup().withEntity(entity).withSetupStatus(setupStatus).withActions(actions);
    }

    /**
     * Build setup.
     *
     * @return the setup
     */
    public Setup build() {
      Setup setup = new Setup();
      setup.setEntity(entity);
      setup.setSetupStatus(setupStatus);
      setup.setActions(actions);
      return setup;
    }
  }
}
