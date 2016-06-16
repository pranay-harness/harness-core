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

  /**
   * Gets field name.
   *
   * @return the field name
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Sets field name.
   *
   * @param fieldName the field name
   */
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  /**
   * Gets order type.
   *
   * @return the order type
   */
  public OrderType getOrderType() {
    return orderType;
  }

  /**
   * Sets order type.
   *
   * @param orderType the order type
   */
  public void setOrderType(OrderType orderType) {
    this.orderType = orderType;
  }

  /**
   * The Enum OrderType.
   */
  public enum OrderType {
    /**
     * Asc order type.
     */
    ASC, /**
          * Desc order type.
          */
    DESC
  }

  public static final class Builder {
    private String fieldName;
    private OrderType orderType;

    private Builder() {}

    public static Builder aSortOrder() {
      return new Builder();
    }

    public Builder withField(String fieldName, OrderType orderType) {
      this.fieldName = fieldName;
      this.orderType = orderType;
      return this;
    }

    public SortOrder build() {
      SortOrder sortOrder = new SortOrder();
      sortOrder.setFieldName(fieldName);
      sortOrder.setOrderType(orderType);
      return sortOrder;
    }
  }
}
