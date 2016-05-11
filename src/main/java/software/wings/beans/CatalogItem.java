/**
 *
 */
package software.wings.beans;

import java.util.Comparator;

/**
 * @author Rishi
 *
 */
public class CatalogItem {
  private String name;
  private String value;
  private String displayText;
  private Integer displayOrder;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDisplayText() {
    if (displayText == null) {
      return name;
    }
    return displayText;
  }

  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
    this.displayOrder = displayOrder;
  }

  @Override
  public String toString() {
    return "CatalogItem [name=" + name + ", value=" + value + ", displayText=" + displayText
        + ", displayOrder=" + displayOrder + "]";
  }

  public static final Comparator<CatalogItem> displayOrderComparator = new Comparator<CatalogItem>() {

    @Override
    public int compare(CatalogItem o1, CatalogItem o2) {
      if (o1.displayOrder == null && o2.displayOrder == null) {
        return 0;
      } else if (o1.displayOrder == null && o2.displayOrder != null) {
        return -1;
      } else if (o1.displayOrder != null && o2.displayOrder == null) {
        return 1;
      } else {
        return o1.displayOrder.compareTo(o2.displayOrder);
      }
    }
  };
}
