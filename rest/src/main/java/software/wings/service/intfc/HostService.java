package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ResponseMessage;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.HostUsage;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.validation.Update;

import java.io.File;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 5/9/16.
 */
public interface HostService {
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
   * Delete by infra.
   *
   * @param infraId the infra id
   */
  void deleteByInfra(String infraId);

  /**
   * Export hosts.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @return the file
   */
  File exportHosts(@NotEmpty String appId, @NotEmpty String infraId);

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
   * Bulk save.
   *
   * @param infraId  the env id
   * @param envId    the env id
   * @param baseHost the base host  @return the response message
   * @return the response message
   */
  ResponseMessage bulkSave(String infraId, String envId, Host baseHost);

  /**
   * Gets host count by infrastructure.
   *
   * @param infraId the infra id
   * @return the host count by infrastructure
   */
  int getInfraHostCount(String infraId);

  /**
   * Gets host usage by application for infrastructure.
   *
   * @param uuid the uuid
   * @return the host usage by application for infrastructure
   */
  List<HostUsage> getInfrastructureHostUsageByApplication(String uuid);

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
   * Gets mapped infra host count.
   *
   * @param infraId the infra id
   * @return the mapped infra host count
   */
  int getMappedInfraHostCount(String infraId);

  /**
   * Delete by environment.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnvironment(String appId, String envId);
}
