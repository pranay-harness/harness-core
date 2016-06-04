package software.wings.beans;

// TODO: Auto-generated Javadoc

/**
 * SortOrder bean class.
 *
 * @author Rishi
 */
public class SortOrder {
  private String fieldName;
  private OrderType orderType;

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public OrderType getOrderType() {
    return orderType;
  }

  public void setOrderType(OrderType orderType) {
    this.orderType = orderType;
  }

  /**
   * The Enum OrderType.
   */
  public enum OrderType { ASC, DESC }
}
