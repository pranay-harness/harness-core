package software.wings.waitnotify;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 12/14/16.
 */
public class ListNotifyResponseData implements NotifyResponseData {
  private List data = new ArrayList();

  public ListNotifyResponseData() {}

  /**
   * Gets data.
   *
   * @return the data
   */
  public List getData() {
    return data;
  }

  /**
   * Adds data.
   *
   * @param data the data
   */
  public void addData(Object data) {
    this.data.add(data);
  }

  /**
   * Sets data.
   *
   * @param data the data
   */
  public void setData(List data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ListNotifyResponseData that = (ListNotifyResponseData) o;

    return data != null ? data.equals(that.data) : that.data == null;
  }

  @Override
  public int hashCode() {
    return data != null ? data.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ListNotifyResponseData{"
        + "data=" + data + '}';
  }

  public static final class ListNotifyResponseDataBuilder {
    private List data = new ArrayList();

    private ListNotifyResponseDataBuilder() {}

    public static ListNotifyResponseDataBuilder aListNotifyResponseData() {
      return new ListNotifyResponseDataBuilder();
    }

    public ListNotifyResponseDataBuilder addData(Object data) {
      this.data.add(data);
      return this;
    }

    public ListNotifyResponseData build() {
      ListNotifyResponseData listNotifyResponseData = new ListNotifyResponseData();
      listNotifyResponseData.setData(data);
      return listNotifyResponseData;
    }
  }
}
