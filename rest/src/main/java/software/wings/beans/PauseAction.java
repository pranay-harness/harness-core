package software.wings.beans;

/**
 * Created by rishi on 10/31/16.
 */
public class PauseAction implements RepairAction {
  private long timeout;

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PauseAction that = (PauseAction) o;

    return timeout == that.timeout;
  }

  @Override
  public int hashCode() {
    return (int) (timeout ^ (timeout >>> 32));
  }

  public static final class PauseActionBuilder {
    private long timeout;

    private PauseActionBuilder() {}

    public static PauseActionBuilder aPauseAction() {
      return new PauseActionBuilder();
    }

    public PauseActionBuilder withTimeout(long timeout) {
      this.timeout = timeout;
      return this;
    }

    public PauseAction build() {
      PauseAction pauseAction = new PauseAction();
      pauseAction.setTimeout(timeout);
      return pauseAction;
    }
  }
}
