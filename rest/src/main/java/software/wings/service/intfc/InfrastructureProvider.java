package software.wings.service.intfc;

import software.wings.beans.ErrorCodes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;

import java.util.List;

/**
 * Created by anubhaw on 10/4/16.
 */
public interface InfrastructureProvider {
  /**
   * Gets all host.
   *
   * @param computeProviderSetting the compute provider setting
   * @return the all host
   */
  PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req);

  /**
   * Save host host.
   *
   * @param host the host
   * @return the host
   */
  default Host
    saveHost(SettingAttribute computeProviderSetting, Host host) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Operation not supported");
    }
}
