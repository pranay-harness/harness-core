package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.beans.SearchFilter.OP;
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
  public static final int DEFAULT_PAGE_SIZE = 50;

  @DefaultValue("0") @QueryParam("offset") private String offset;

  private int start;

  @DefaultValue("50") @QueryParam("limit") private String limit;

  private int pageSize = DEFAULT_PAGE_SIZE;
  private List<SearchFilter> filters = new ArrayList<SearchFilter>();
  private List<SortOrder> orders = new ArrayList<>();
  private List<String> fieldsIncluded = new ArrayList<>();
  private List<String> fieldsExcluded = new ArrayList<>();

  @JsonIgnore @Context private UriInfo uriInfo;

  @JsonIgnore private boolean isOr = false;

  public PageRequest() {}

  /**
   * Copy Constructor for PageRequest
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
    return fieldsIncluded;
  }

  public void setFieldsIncluded(List<String> fieldsIncluded) {
    this.fieldsIncluded = fieldsIncluded;
  }

  public List<String> getFieldsExcluded() {
    return fieldsExcluded;
  }

  public void setFieldsExcluded(List<String> fieldsExcluded) {
    this.fieldsExcluded = fieldsExcluded;
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
        filter.setOp(OP.valueOf(map.getFirst(key + "[op]")));
      }
      // if(map.containsKey(key + "[dataType]")){
      //  filter.setDataType(map.getFirst(key + "[dataType]"));
      //}
      if (map.containsKey(key + "[value]")) {
        filter.setFieldValue(map.getFirst(key + "[value]"));
      }
      getFilters().add(filter);
    }
    for (int index = 0; index < orderCount; index++) {
      String key = "sort[" + index + "]";
      SortOrder order = new SortOrder();
      order.setFieldName(map.getFirst(key + "[field]"));
      order.setOrderType(OrderType.valueOf(map.getFirst(key + "[direction]").toUpperCase()));
      getOrders().add(order);
    }
  }

  public List<SearchFilter> getFilters() {
    return filters;
  }

  public void setFilters(List<SearchFilter> filters) {
    this.filters = filters;
  }

  public List<SortOrder> getOrders() {
    return orders;
  }

  public void setOrders(List<SortOrder> orders) {
    this.orders = orders;
  }

  /**
   * Creates and adds a new search filter to PageRequest
   * @param fieldName name of field to apply filter on.
   * @param fieldValue value for RHS.
   * @param op Operator for filter.
   */
  public void addFilter(String fieldName, Object fieldValue, OP op) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName(fieldName);
    filter.setFieldValue(fieldValue);
    filter.setOp(op);
    getFilters().add(filter);
  }
}
