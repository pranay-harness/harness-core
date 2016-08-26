package software.wings.beans;

/**
 * Created by peeyushaggarwal on 7/8/16.
 */
public class CountsByStatuses {
  private int success;
  private int failed;
  private int inprogress;
  private int queued;

  /**
   * Gets success.
   *
   * @return the success
   */
  public int getSuccess() {
    return success;
  }

  /**
   * Sets success.
   *
   * @param success the success
   */
  public void setSuccess(int success) {
    this.success = success;
  }

  /**
   * Gets failed.
   *
   * @return the failed
   */
  public int getFailed() {
    return failed;
  }

  /**
   * Sets failed.
   *
   * @param failed the failed
   */
  public void setFailed(int failed) {
    this.failed = failed;
  }

  /**
   * Getter for property 'inprogress'.
   *
   * @return Value for property 'inprogress'.
   */
  public int getInprogress() {
    return inprogress;
  }

  public int getQueued() {
    return queued;
  }

  public void setQueued(int queued) {
    this.queued = queued;
  }

  /**
   * Setter for property 'inprogress'.
   *
   * @param inprogress Value to set for property 'inprogress'.
   */
  public void setInprogress(int inprogress) {
    this.inprogress = inprogress;
  }

  @Override
  public String toString() {
    return "CountsByStatuses{"
        + "success=" + success + ", failed=" + failed + ", inprogress=" + inprogress + ", queued=" + queued + '}';
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int success;
    private int failed;
    private int inprogress;
    private int queued;

    private Builder() {}

    public static Builder aCountsByStatuses() {
      return new Builder();
    }

    public Builder withSuccess(int success) {
      this.success = success;
      return this;
    }

    public Builder withFailed(int failed) {
      this.failed = failed;
      return this;
    }

    public Builder withInprogress(int inprogress) {
      this.inprogress = inprogress;
      return this;
    }

    public Builder withQueued(int queued) {
      this.queued = queued;
      return this;
    }

    public CountsByStatuses build() {
      CountsByStatuses countsByStatuses = new CountsByStatuses();
      countsByStatuses.setSuccess(success);
      countsByStatuses.setFailed(failed);
      countsByStatuses.setInprogress(inprogress);
      countsByStatuses.setQueued(queued);
      return countsByStatuses;
    }
  }
}
