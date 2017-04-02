package software.wings.waitnotify;

/**
 * Created by peeyushaggarwal on 3/31/17.
 */
public class ErrorNotifyResponseData implements NotifyResponseData {
  private String errorMessage;

  /**
   * Getter for property 'errorMessage'.
   *
   * @return Value for property 'errorMessage'.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Setter for property 'errorMessage'.
   *
   * @param errorMessage Value to set for property 'errorMessage'.
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public static final class Builder {
    private String errorMessage;

    private Builder() {}

    public static Builder anErrorNotifyResponseData() {
      return new Builder();
    }

    public Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder but() {
      return anErrorNotifyResponseData().withErrorMessage(errorMessage);
    }

    public ErrorNotifyResponseData build() {
      ErrorNotifyResponseData errorNotifyResponseData = new ErrorNotifyResponseData();
      errorNotifyResponseData.setErrorMessage(errorMessage);
      return errorNotifyResponseData;
    }
  }
}
