package software.wings.beans;

import java.util.List;

/**
 *  PageResponse bean class.
 *
 *
 * @author Rishi
 *
 */
public class PageResponse<T> extends PageRequest<T> {
  private List<T> response;
  private long total;

  public PageResponse() {}

  public PageResponse(PageRequest<T> req) {
    super(req);
  }
  public List<T> getResponse() {
    return response;
  }
  public void setResponse(List<T> response) {
    this.response = response;
  }
  public long getTotal() {
    return total;
  }
  public void setTotal(long total) {
    this.total = total;
  }

  public long getCurrentPage() {
    if (getPageSize() == 0) {
      return 0;
    }
    int pageno = getStart() / getPageSize();
    if (getStart() % getPageSize() == 0) {
      pageno++;
    }
    return pageno;
  }
}
