package software.wings.service.intfc;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 10/4/16.
 */
public interface InfrastructureProvider {
  /**
   * Gets all host.
   *
   * @param computeProviderSetting the compute provider setting
   * @param req                    the req
   * @return the all host
   */
  PageResponse<Host> listHosts(String region, SettingAttribute computeProviderSetting, PageRequest<Host> req);

  /**
   * Save host host.
   *
   * @param host the host
   * @return the host
   */
  Host saveHost(Host host);

  /**
   * Delete host.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @param publicDns       the public dns
   */
  void deleteHost(String appId, String infraMappingId, String publicDns);

  /**
   * Update host conn attrs.
   *
   * @param infrastructureMapping the infrastructure mapping
   * @param hostConnectionAttrs   the host connection attrs
   */
  void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs);

  /**
   * Delete host by infra mapping id.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   */
  void deleteHostByInfraMappingId(String appId, String infraMappingId);
}
