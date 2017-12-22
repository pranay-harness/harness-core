package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/9/16.
 */
public interface HostService extends OwnedByInfrastructureMapping {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<Host> list(PageRequest<Host> req);

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the host
   */
  Host get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId);

  /**
   * Update.
   *
   * @param envId the env id
   * @param host  the host
   * @return the host
   */
  @ValidationGroups(Update.class) Host update(String envId, @Valid Host host);

  /**
   * Delete.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId);

  /**
   * Prune owned from the app objects.
   *
   * @param appId the app id
   * @param hostId the host id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String hostId);

  /**
   * Import hosts.
   *
   * @param infraId            the infra id
   * @param appId              the app id
   * @param envId              the infra id
   * @param boundedInputStream the bounded input stream
   * @return the int
   */
  int importHosts(@NotEmpty String infraId, @NotEmpty String appId, @NotEmpty String envId,
      @NotNull BoundedInputStream boundedInputStream);

  /**
   * Gets the hosts by id.
   *
   * @param appId     the app id
   * @param envId     the env id
   * @param hostUuids the host uuids
   * @return the hosts by id
   */
  List<Host> getHostsByHostIds(@NotEmpty String appId, @NotEmpty String envId, @NotNull List<String> hostUuids);

  /**
   * Gets hosts by env.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the hosts by env
   */
  List<Host> getHostsByEnv(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Gets host by env.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the host by env
   */
  Host getHostByEnv(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId);

  /**
   * Save application host application host.
   *
   * @param appHost the app host
   * @return the application host
   */
  Host saveHost(Host appHost);

  /**
   * Exist boolean.
   *
   * @param appId  the app id
   * @param hostId the host id
   * @return the boolean
   */
  boolean exist(String appId, String hostId);

  /**
   * Delete by environment.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnvironment(String appId, String envId);

  /**
   * Delete by host name.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @param dnsName        the dns name
   */
  void deleteByDnsName(String appId, String infraMappingId, String dnsName);

  /**
   * Update host connection attr by infra mapping id.
   *
   * @param appId               the app id
   * @param infraMappingId      the infra mapping id
   * @param hostConnectionAttrs the host connection attrs
   */
  void updateHostConnectionAttrByInfraMappingId(String appId, String infraMappingId, String hostConnectionAttrs);

  /**
   * Delete by service.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceTemplateId the service template id
   */
  void deleteByService(String appId, String envId, String serviceTemplateId);
}
