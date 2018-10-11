package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import io.harness.task.protocol.ResponseData;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/24/16.
 */
public class StringNotifyResponseData implements ResponseData {
  private String data;

  public StringNotifyResponseData() {}

  /**
   * Gets data.
   *
   * @return the data
   */
  public String getData() {
    return data;
  }

  /**
   * Sets data.
   *
   * @param data the data
   */
  public void setData(String data) {
    this.data = data;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StringNotifyResponseData other = (StringNotifyResponseData) obj;
    return Objects.equals(this.data, other.data);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("data", data).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String data;

    private Builder() {}

    /**
     * A string notify response data builder.
     *
     * @return the builder
     */
    public static Builder aStringNotifyResponseData() {
      return new Builder();
    }

    /**
     * With data builder.
     *
     * @param data the data
     * @return the builder
     */
    public Builder withData(String data) {
      this.data = data;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aStringNotifyResponseData().withData(data);
    }

    /**
     * Build string notify response data.
     *
     * @return the string notify response data
     */
    public StringNotifyResponseData build() {
      StringNotifyResponseData stringNotifyResponseData = new StringNotifyResponseData();
      stringNotifyResponseData.setData(data);
      return stringNotifyResponseData;
    }
  }
}
