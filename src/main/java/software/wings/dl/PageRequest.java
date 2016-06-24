package software.wings.dl;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.mapping.MappedClass;
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

// TODO: Auto-generated Javadoc

/**
 * PageRequest bean class.
 *
 * @param <T> the generic type
 * @author Rishi
 */
public class PageRequest<T> {
  /**
   * The constant UNLIMITED.
   */
  public static final String UNLIMITED = "UNLIMITED";
  /**
   * The constant DEFAULT_UNLIMITED.
   */
  public static final int DEFAULT_UNLIMITED = 1000;
  /**
   * The constant DEFAULT_PAGE_SIZE.
   */
  public static final int DEFAULT_PAGE_SIZE = 50;
  /**
   * The Persistent class.
   */
  @JsonIgnore Class<T> persistentClass;
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

  /**
   * Instantiates a new page request.
   */
  public PageRequest() {}

  /**
   * Copy Constructor for PageRequest.
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

  /**
   * Gets uri info.
   *
   * @return the uri info
   */
  public UriInfo getUriInfo() {
    return uriInfo;
  }

  /**
   * Sets uri info.
   *
   * @param uriInfo the uri info
   */
  public void setUriInfo(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }

  /**
   * Gets offset.
   *
   * @return the offset
   */
  public String getOffset() {
    return offset;
  }

  /**
   * Sets offset.
   *
   * @param offset the offset
   */
  public void setOffset(String offset) {
    this.offset = offset;
  }

  /**
   * Gets limit.
   *
   * @return the limit
   */
  public String getLimit() {
    return limit;
  }

  /**
   * Sets limit.
   *
   * @param limit the limit
   */
  public void setLimit(String limit) {
    this.limit = limit;
  }

  /**
   * Gets page size.
   *
   * @return the page size
   */
  public int getPageSize() {
    return Misc.asInt(limit, DEFAULT_PAGE_SIZE);
  }

  /**
   * Gets start.
   *
   * @return the start
   */
  public int getStart() {
    return Misc.asInt(offset);
  }

  /**
   * Is or boolean.
   *
   * @return the boolean
   */
  public boolean isOr() {
    return isOr;
  }

  /**
   * Sets or.
   *
   * @param isOr the is or
   */
  public void setOr(boolean isOr) {
    this.isOr = isOr;
  }

  /**
   * Gets fields included.
   *
   * @return the fields included
   */
  public List<String> getFieldsIncluded() {
    return new ArrayList<>(fieldsIncluded);
  }

  /**
   * Sets fields included.
   *
   * @param fieldsIncluded the fields included
   */
  public void setFieldsIncluded(List<String> fieldsIncluded) {
    this.fieldsIncluded = fieldsIncluded;
  }

  /**
   * Adds the fields included.
   *
   * @param fieldsIncluded the fields included
   */
  public void addFieldsIncluded(String fieldsIncluded) {
    this.fieldsIncluded.add(fieldsIncluded);
  }

  /**
   * Gets fields excluded.
   *
   * @return the fields excluded
   */
  public List<String> getFieldsExcluded() {
    return new ArrayList<>(fieldsExcluded);
  }

  /**
   * Sets fields excluded.
   *
   * @param fieldsExcluded the fields excluded
   */
  public void setFieldsExcluded(List<String> fieldsExcluded) {
    this.fieldsExcluded = fieldsExcluded;
  }

  /**
   * Adds the fields excluded.
   *
   * @param fieldsExcluded the fields excluded
   */
  public void addFieldsExcluded(String fieldsExcluded) {
    this.fieldsExcluded.add(fieldsExcluded);
  }

  /**
   * Converts the filter to morphia form.
   *
   * @param mappedClass the mapped class
   */
  public void populateFilters(MappedClass mappedClass) {
    if (uriInfo == null) {
      return;
    }
    MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
    int fieldCount = 0;
    int orderCount = 0;
    for (String key : map.keySet()) {
      if (key.startsWith("search") && key.endsWith("[field]")) {
        fieldCount++;
      } else if (key.startsWith("sort") && key.endsWith("[field]")) {
        orderCount++;
      } else if (key.equals("searchLogic")) {
        String searchLogic = map.getFirst("searchLogic");
        searchLogic = searchLogic == null ? "" : searchLogic.trim();
        if (searchLogic.equals("OR")) {
          isOr = true;
        }
      } else if (!(key.startsWith("search") || key.startsWith("sort") || mappedClass.getMappedField(key) == null)) {
        filters.add(aSearchFilter().withField(key, Operator.IN, map.get(key).toArray()).build());
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

  /**
   * Gets filters.
   *
   * @return the filters
   */
  public List<SearchFilter> getFilters() {
    return new ArrayList<>(filters);
  }

  /**
   * Sets filters.
   *
   * @param filters the filters
   */
  public void setFilters(List<SearchFilter> filters) {
    this.filters = filters;
  }

  /**
   * Adds the filter.
   *
   * @param filter the filter
   */
  public void addFilter(SearchFilter filter) {
    this.filters.add(filter);
  }

  /**
   * Gets orders.
   *
   * @return the orders
   */
  public List<SortOrder> getOrders() {
    return new ArrayList<>(orders);
  }

  /**
   * Sets orders.
   *
   * @param orders the orders
   */
  public void setOrders(List<SortOrder> orders) {
    this.orders = orders;
  }

  /**
   * Adds the order.
   *
   * @param order the order
   */
  public void addOrder(SortOrder order) {
    this.orders.add(order);
  }

  /**
   * Creates and adds a new search filter to PageRequest.
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

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(offset, start, limit, pageSize, filters, orders, fieldsIncluded, fieldsExcluded, isOr);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String offset;
    private String limit;
    private List<SearchFilter> filters = new ArrayList<SearchFilter>();
    private List<SortOrder> orders = new ArrayList<>();
    private List<String> fieldsIncluded = new ArrayList<>();
    private List<String> fieldsExcluded = new ArrayList<>();
    private UriInfo uriInfo;

    private Builder() {}

    /**
     * A page request.
     *
     * @return the builder
     */
    public static Builder aPageRequest() {
      return new Builder();
    }

    /**
     * With offset.
     *
     * @param offset the offset
     * @return the builder
     */
    public Builder withOffset(String offset) {
      this.offset = offset;
      return this;
    }

    /**
     * With limit.
     *
     * @param limit the limit
     * @return the builder
     */
    public Builder withLimit(String limit) {
      this.limit = limit;
      return this;
    }

    /**
     * Adds the filter.
     *
     * @param filter the filter
     * @return the builder
     */
    public Builder addFilter(SearchFilter filter) {
      this.filters.add(filter);
      return this;
    }

    /**
     * Adds the order.
     *
     * @param order the order
     * @return the builder
     */
    public Builder addOrder(SortOrder order) {
      this.orders.add(order);
      return this;
    }

    /**
     * With fields included.
     *
     * @param fieldsIncluded the fields included
     * @return the builder
     */
    public Builder withFieldsIncluded(List<String> fieldsIncluded) {
      this.fieldsIncluded = fieldsIncluded;
      return this;
    }

    /**
     * With fields excluded.
     *
     * @param fieldsExcluded the fields excluded
     * @return the builder
     */
    public Builder withFieldsExcluded(List<String> fieldsExcluded) {
      this.fieldsExcluded = fieldsExcluded;
      return this;
    }

    /**
     * With uri info.
     *
     * @param uriInfo the uri info
     * @return the builder
     */
    public Builder withUriInfo(UriInfo uriInfo) {
      this.uriInfo = uriInfo;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the page request
     */
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
