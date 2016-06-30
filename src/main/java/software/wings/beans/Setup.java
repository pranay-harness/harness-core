package software.wings.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 6/29/16.
 */
public class Setup {
  public enum SetupStatus { COMPLETE, INCOMPLETE }

  private Base entity;
  private SetupStatus setupStatus;
  private List<SetupAction> actions = new ArrayList<>();

  public Base getEntity() {
    return entity;
  }

  public void setEntity(Base entity) {
    this.entity = entity;
  }

  public SetupStatus getSetupStatus() {
    return setupStatus;
  }

  public void setSetupStatus(SetupStatus setupStatus) {
    this.setupStatus = setupStatus;
  }

  public List<SetupAction> getActions() {
    return actions;
  }

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

  public static final class Builder {
    private Base entity;
    private SetupStatus setupStatus;
    private List<SetupAction> actions = new ArrayList<>();

    private Builder() {}

    public static Builder aSetup() {
      return new Builder();
    }

    public Builder withEntity(Base entity) {
      this.entity = entity;
      return this;
    }

    public Builder withSetupStatus(SetupStatus setupStatus) {
      this.setupStatus = setupStatus;
      return this;
    }

    public Builder addActions(SetupAction... actions) {
      Collections.addAll(this.actions, actions);
      return this;
    }

    public Builder withActions(List<SetupAction> actions) {
      this.actions = actions;
      return this;
    }

    public Builder but() {
      return aSetup().withEntity(entity).withSetupStatus(setupStatus).withActions(actions);
    }

    public Setup build() {
      Setup setup = new Setup();
      setup.setEntity(entity);
      setup.setSetupStatus(setupStatus);
      setup.setActions(actions);
      return setup;
    }
  }
}
