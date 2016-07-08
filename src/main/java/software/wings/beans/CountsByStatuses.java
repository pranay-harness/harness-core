package software.wings.beans;

/**
 * Created by peeyushaggarwal on 7/8/16.
 */
public class CountsByStatuses {
  private int success;
  private int failed;
  private int inprogress;

  public int getSuccess() {
    return success;
  }

  public void setSuccess(int success) {
    this.success = success;
  }

  public int getFailed() {
    return failed;
  }

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

  public static final class Builder {
    private int success;
    private int failed;
    private int inprogress;

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

    public CountsByStatuses build() {
      CountsByStatuses countsByStatuses = new CountsByStatuses();
      countsByStatuses.setSuccess(success);
      countsByStatuses.setFailed(failed);
      countsByStatuses.setInprogress(inprogress);
      return countsByStatuses;
    }
  }
}
