package software.wings.dl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * PageRequest bean class.
 *
 * @author Rishi
 */
public class PageRequest<T> {
  public static final String UNLIMITED = "UNLIMITED";
  public static final int DEFAULT_UNLIMITED = 1000;
  public static final int DEFAULT_PAGE_SIZE = 50;

  @DefaultValue("0") @QueryParam("offset") private String offset;

  private int start;

  @DefaultValue("50") @QueryParam("limit") private String limit;

  private int pageSize = DEFAULT_PAGE_SIZE;
  private List<SearchFilter> filters = new ArrayList<>();
  private List<SortOrder> orders = new ArrayList<>();
  private List<String> fieldsIncluded = new ArrayList<>();
  private List<String> fieldsExcluded = new ArrayList<>();

  @JsonIgnore @Context private UriInfo uriInfo;

  @JsonIgnore private boolean isOr = false;

  public PageRequest() {}

  /**
   * Copy Constructor for PageRequest
   *
   * @param req PageRequest to copy.
   */
  public PageRequest(PageRequest<T> req) {
    this.offset = req.offset;
    this.pageSize = req.pageSize;
    this.start = req.start;
    this.limit = req.limit;
    this.filters = req.filters;
    this.orders = req.orders;
    this.isOr = req.isOr;
    this.fieldsIncluded = req.fieldsIncluded;
    this.fieldsExcluded = req.fieldsExcluded;
  }

  public UriInfo getUriInfo() {
    return uriInfo;
  }

  public void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  public String getOffset() {
    return offset;
  }

  public void setOffset(String offset) {
    this.offset = offset;
  }

  public String getLimit() {
    return limit;
  }

  public void setLimit(String limit) {
    this.limit = limit;
  }

  public int getPageSize() {
    return Misc.asInt(limit, DEFAULT_PAGE_SIZE);
  }

  public int getStart() {
    return Misc.asInt(offset);
  }

  public boolean isOr() {
    return isOr;
  }

  public void setOr(boolean isOr) {
    this.isOr = isOr;
  }

  public List<String> getFieldsIncluded() {
    return new ArrayList<>(fieldsIncluded);
  }

  public void setFieldsIncluded(List<String> fieldsIncluded) {
    this.fieldsIncluded = fieldsIncluded;
  }

  public void addFieldsIncluded(String fieldsIncluded) {
    this.fieldsIncluded.add(fieldsIncluded);
  }

  public List<String> getFieldsExcluded() {
    return new ArrayList<>(fieldsExcluded);
  }

  public void setFieldsExcluded(List<String> fieldsExcluded) {
    this.fieldsExcluded = fieldsExcluded;
  }

  public void addFieldsExcluded(String fieldsExcluded) {
    this.fieldsExcluded.add(fieldsExcluded);
  }

  /**
   * Converts the filter to morphia form.
   */
  public void populateFilters() {
    if (uriInfo == null) {
      return;
    }
    MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
    int fieldCount = 0;
    int orderCount = 0;
    for (String key : map.keySet()) {
      if (key.startsWith("search") && key.endsWith("[field]")) {
        fieldCount++;
      }
      if (key.startsWith("sort") && key.endsWith("[field]")) {
        orderCount++;
      }
      if (key.equals("searchLogic")) {
        String searchLogic = map.getFirst("searchLogic");
        searchLogic = searchLogic == null ? "" : searchLogic.trim();
        if (searchLogic.equals("OR")) {
          isOr = true;
        }
      }
    }
    for (int index = 0; index < fieldCount; index++) {
      String key = "search[" + index + "]";
      SearchFilter filter = new SearchFilter();
      filter.setFieldName(map.getFirst(key + "[field]"));
      if (map.containsKey(key + "[op]")) {
        filter.setOp(Operator.valueOf(map.getFirst(key + "[op]")));
      }
      // if(map.containsKey(key + "[dataType]")){
      //  filter.setDataType(map.getFirst(key + "[dataType]"));
      //}
      if (map.containsKey(key + "[value]")) {
        filter.setFieldValues(map.getFirst(key + "[value]"));
      }
      filters.add(filter);
    }
    for (int index = 0; index < orderCount; index++) {
      String key = "sort[" + index + "]";
      SortOrder order = new SortOrder();
      order.setFieldName(map.getFirst(key + "[field]"));
      order.setOrderType(OrderType.valueOf(map.getFirst(key + "[direction]").toUpperCase()));
      orders.add(order);
    }
  }

  public List<SearchFilter> getFilters() {
    return new ArrayList<>(filters);
  }

  public void setFilters(List<SearchFilter> filters) {
    this.filters = filters;
  }

  public void addFilter(SearchFilter filter) {
    this.filters.add(filter);
  }

  public List<SortOrder> getOrders() {
    return new ArrayList<>(orders);
  }

  public void setOrders(List<SortOrder> orders) {
    this.orders = orders;
  }

  public void addOrder(SortOrder order) {
    this.orders.add(order);
  }

  /**
   * Creates and adds a new search filter to PageRequest
   *
   * @param fieldName  name of field to apply filter on.
   * @param fieldValue value for RHS.
   * @param op         Operator for filter.
   */
  public void addFilter(String fieldName, Object fieldValue, Operator op) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName(fieldName);
    filter.setFieldValues(fieldValue);
    filter.setOp(op);
    filters.add(filter);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PageRequest<?> that = (PageRequest<?>) obj;
    return start == that.start && pageSize == that.pageSize && isOr == that.isOr && Objects.equal(offset, that.offset)
        && Objects.equal(limit, that.limit) && Objects.equal(filters, that.filters)
        && Objects.equal(orders, that.orders) && Objects.equal(fieldsIncluded, that.fieldsIncluded)
        && Objects.equal(fieldsExcluded, that.fieldsExcluded);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(offset, start, limit, pageSize, filters, orders, fieldsIncluded, fieldsExcluded, isOr);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("isOr", isOr)
        .add("uriInfo", uriInfo)
        .add("fieldsExcluded", fieldsExcluded)
        .add("fieldsIncluded", fieldsIncluded)
        .add("orders", orders)
        .add("filters", filters)
        .add("pageSize", pageSize)
        .add("limit", limit)
        .add("start", start)
        .add("offset", offset)
        .toString();
  }

  public static final class Builder {
    private String offset;
    private String limit;
    private List<SearchFilter> filters = new ArrayList<SearchFilter>();
    private List<SortOrder> orders = new ArrayList<>();
    private List<String> fieldsIncluded = new ArrayList<>();
    private List<String> fieldsExcluded = new ArrayList<>();
    private UriInfo uriInfo;

    private Builder() {}

    public static Builder aPageRequest() {
      return new Builder();
    }

    public Builder withOffset(String offset) {
      this.offset = offset;
      return this;
    }

    public Builder withLimit(String limit) {
      this.limit = limit;
      return this;
    }

    public Builder addFilter(SearchFilter filter) {
      this.filters.add(filter);
      return this;
    }

    public Builder addOrder(SortOrder order) {
      this.orders.add(order);
      return this;
    }

    public Builder withFieldsIncluded(List<String> fieldsIncluded) {
      this.fieldsIncluded = fieldsIncluded;
      return this;
    }

    public Builder withFieldsExcluded(List<String> fieldsExcluded) {
      this.fieldsExcluded = fieldsExcluded;
      return this;
    }

    public Builder withUriInfo(UriInfo uriInfo) {
      this.uriInfo = uriInfo;
      return this;
    }

    public PageRequest build() {
      PageRequest pageRequest = new PageRequest();
      pageRequest.setOffset(offset);
      pageRequest.setLimit(limit);
      pageRequest.setFilters(filters);
      pageRequest.setOrders(orders);
      pageRequest.setFieldsIncluded(fieldsIncluded);
      pageRequest.setFieldsExcluded(fieldsExcluded);
      pageRequest.setUriInfo(uriInfo);
      return pageRequest;
    }
  }
}
