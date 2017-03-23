package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
@Entity(value = "licenses", noClassnameStored = true)
public class License extends Base {
  private String name;
  private List<Entitlement> entitlements;
  private boolean isActive;
  private long expiryDuration;

  public static class Entitlement {
    private String operation;
    private int limit;
    private int rate;
    private TimeUnit rateUnit;

    /**
     * Getter for property 'operation'.
     *
     * @return Value for property 'operation'.
     */
    public String getOperation() {
      return operation;
    }

    /**
     * Setter for property 'operation'.
     *
     * @param operation Value to set for property 'operation'.
     */
    public void setOperation(String operation) {
      this.operation = operation;
    }

    /**
     * Getter for property 'limit'.
     *
     * @return Value for property 'limit'.
     */
    public int getLimit() {
      return limit;
    }

    /**
     * Setter for property 'limit'.
     *
     * @param limit Value to set for property 'limit'.
     */
    public void setLimit(int limit) {
      this.limit = limit;
    }

    /**
     * Getter for property 'rate'.
     *
     * @return Value for property 'rate'.
     */
    public int getRate() {
      return rate;
    }

    /**
     * Setter for property 'rate'.
     *
     * @param rate Value to set for property 'rate'.
     */
    public void setRate(int rate) {
      this.rate = rate;
    }

    /**
     * Getter for property 'rateUnit'.
     *
     * @return Value for property 'rateUnit'.
     */
    public TimeUnit getRateUnit() {
      return rateUnit;
    }

    /**
     * Setter for property 'rateUnit'.
     *
     * @param rateUnit Value to set for property 'rateUnit'.
     */
    public void setRateUnit(TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
    }
  }

  /**
   * Getter for property 'name'.
   *
   * @return Value for property 'name'.
   */
  public String getName() {
    return name;
  }

  /**
   * Setter for property 'name'.
   *
   * @param name Value to set for property 'name'.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Getter for property 'entitlements'.
   *
   * @return Value for property 'entitlements'.
   */
  public List<Entitlement> getEntitlements() {
    return entitlements;
  }

  /**
   * Setter for property 'entitlements'.
   *
   * @param entitlements Value to set for property 'entitlements'.
   */
  public void setEntitlements(List<Entitlement> entitlements) {
    this.entitlements = entitlements;
  }

  /**
   * Getter for property 'active'.
   *
   * @return Value for property 'active'.
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Setter for property 'active'.
   *
   * @param active Value to set for property 'active'.
   */
  public void setActive(boolean active) {
    isActive = active;
  }

  /**
   * Getter for property 'expiryDuration'.
   *
   * @return Value for property 'expiryDuration'.
   */
  public long getExpiryDuration() {
    return expiryDuration;
  }

  /**
   * Setter for property 'expiryDuration'.
   *
   * @param expiryDuration Value to set for property 'expiryDuration'.
   */
  public void setExpiryDuration(long expiryDuration) {
    this.expiryDuration = expiryDuration;
  }
}
