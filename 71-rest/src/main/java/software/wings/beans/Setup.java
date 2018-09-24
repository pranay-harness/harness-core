package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 6/29/16.
 */
public class Setup {
  private SetupStatus setupStatus;
  private List<SetupAction> actions = new ArrayList<>();

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
    return Objects.hash(setupStatus, actions);
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
    return Objects.equals(this.setupStatus, other.setupStatus) && Objects.equals(this.actions, other.actions);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("setupStatus", setupStatus).add("actions", actions).toString();
  }

  /**
   * The enum Setup status.
   */
  public enum SetupStatus {
    /**
     * Complete setup status.
     */
    COMPLETE,
    /**
     * Incomplete setup status.
     */
    INCOMPLETE
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
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
      return aSetup().withSetupStatus(setupStatus).withActions(actions);
    }

    /**
     * Build setup.
     *
     * @return the setup
     */
    public Setup build() {
      Setup setup = new Setup();
      setup.setSetupStatus(setupStatus);
      setup.setActions(actions);
      return setup;
    }
  }
}
