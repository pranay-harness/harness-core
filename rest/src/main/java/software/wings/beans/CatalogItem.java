/**
 *
 */

package software.wings.beans;

import java.util.Comparator;

/**
 * The Class CatalogItem.
 *
 * @author Rishi
 */
public class CatalogItem {
  /**
   * The constant displayOrderComparator.
   */
  public static final Comparator<CatalogItem> displayOrderComparator = new Comparator<CatalogItem>() {
    @Override
    public int compare(CatalogItem o1, CatalogItem o2) {
      if (o1.displayOrder == null && o2.displayOrder == null) {
        return 0;
      } else if (o1.displayOrder != null && o2.displayOrder != null) {
        return o1.displayOrder.compareTo(o2.displayOrder);
      } else if (o1.displayOrder == null && o2.displayOrder != null) {
        return -1;
      } else {
        return 1;
      }
    }
  };
  private String name;
  private String value;
  private String displayText;
  private Integer displayOrder;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets value.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Gets display text.
   *
   * @return the display text
   */
  public String getDisplayText() {
    if (displayText == null) {
      return name;
    }
    return displayText;
  }

  /**
   * Sets display text.
   *
   * @param displayText the display text
   */
  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  /**
   * Gets display order.
   *
   * @return the display order
   */
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  /**
   * Sets display order.
   *
   * @param displayOrder the display order
   */
  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "CatalogItem [name=" + name + ", value=" + value + ", displayText=" + displayText
        + ", displayOrder=" + displayOrder + "]";
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String value;
    private String displayText;
    private Integer displayOrder;

    private Builder() {}

    /**
     * A catalog item builder.
     *
     * @return the builder
     */
    public static Builder aCatalogItem() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With value builder.
     *
     * @param value the value
     * @return the builder
     */
    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    /**
     * With display text builder.
     *
     * @param displayText the display text
     * @return the builder
     */
    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
      return this;
    }

    /**
     * With display order builder.
     *
     * @param displayOrder the display order
     * @return the builder
     */
    public Builder withDisplayOrder(Integer displayOrder) {
      this.displayOrder = displayOrder;
      return this;
    }

    /**
     * Build catalog item.
     *
     * @return the catalog item
     */
    public CatalogItem build() {
      CatalogItem catalogItem = new CatalogItem();
      catalogItem.setName(name);
      catalogItem.setValue(value);
      catalogItem.setDisplayText(displayText);
      catalogItem.setDisplayOrder(displayOrder);
      return catalogItem;
    }
  }
}
