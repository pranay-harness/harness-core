package software.wings.beans;

import java.util.Objects;

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

  /**
   * Setter for property 'inprogress'.
   *
   * @param inprogress Value to set for property 'inprogress'.
   */
  public void setInprogress(int inprogress) {
    this.inprogress = inprogress;
  }

  /**
   * Gets queued.
   *
   * @return the queued
   */
  public int getQueued() {
    return queued;
  }

  /**
   * Sets queued.
   *
   * @param queued the queued
   */
  public void setQueued(int queued) {
    this.queued = queued;
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, failed, inprogress, queued);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CountsByStatuses other = (CountsByStatuses) obj;
    return Objects.equals(this.success, other.success) && Objects.equals(this.failed, other.failed)
        && Objects.equals(this.inprogress, other.inprogress) && Objects.equals(this.queued, other.queued);
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

    /**
     * A counts by statuses builder.
     *
     * @return the builder
     */
    public static Builder aCountsByStatuses() {
      return new Builder();
    }

    /**
     * With success builder.
     *
     * @param success the success
     * @return the builder
     */
    public Builder withSuccess(int success) {
      this.success = success;
      return this;
    }

    /**
     * With failed builder.
     *
     * @param failed the failed
     * @return the builder
     */
    public Builder withFailed(int failed) {
      this.failed = failed;
      return this;
    }

    /**
     * With inprogress builder.
     *
     * @param inprogress the inprogress
     * @return the builder
     */
    public Builder withInprogress(int inprogress) {
      this.inprogress = inprogress;
      return this;
    }

    /**
     * With queued builder.
     *
     * @param queued the queued
     * @return the builder
     */
    public Builder withQueued(int queued) {
      this.queued = queued;
      return this;
    }

    /**
     * Build counts by statuses.
     *
     * @return the counts by statuses
     */
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
