/**
 *
 */
package software.wings.sm;

import software.wings.beans.Service;

/**
 * @author Rishi
 *
 */
public class ServiceElement implements RepeatElement {
  private static final long serialVersionUID = 1300265460164938418L;

  private Service service;

  @Override
  public RepeatElementType getRepeatElementType() {
    return RepeatElementType.SERVICE;
  }

  @Override
  public String getRepeatElementName() {
    return service.getName();
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }
}
