package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.ResponseMessage;
import software.wings.beans.Tag;
import software.wings.beans.infrastructure.ApplicationHostUsage;
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
  PageResponse<ApplicationHost> list(PageRequest<ApplicationHost> req);

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the host
   */
  ApplicationHost get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId);

  /**
   * Update.
   *
   * @param envId the env id
   * @param host  the host
   * @return the host
   */
  @ValidationGroups(Update.class) ApplicationHost update(String envId, @Valid Host host);

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
  List<ApplicationHost> getHostsByHostIds(
      @NotEmpty String appId, @NotEmpty String envId, @NotNull List<String> hostUuids);

  /**
   * Gets the hosts by tags.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tags  the tags
   * @return the hosts by tags
   */
  List<ApplicationHost> getHostsByTags(@NotEmpty String appId, @NotEmpty String envId, @NotNull List<Tag> tags);

  /**
   * Sets tag.
   *
   * @param host the host
   * @param tag  the tag
   */
  void setTag(@NotNull ApplicationHost host, @NotNull Tag tag);

  /**
   * Remove tag from host.
   *
   * @param host the host
   * @param tag  the tag
   */
  void removeTagFromHost(@NotNull ApplicationHost host, @NotNull Tag tag);

  /**
   * Gets hosts by env.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the hosts by env
   */
  List<ApplicationHost> getHostsByEnv(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Gets host by env.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the host by env
   */
  ApplicationHost getHostByEnv(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String hostId);

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
  int getHostCountByInfrastructure(String infraId);

  /**
   * Gets host usage by application for infrastructure.
   *
   * @param uuid the uuid
   * @return the host usage by application for infrastructure
   */
  List<ApplicationHostUsage> getInfrastructureHostUsageByApplication(String uuid);

  /**
   * Add application host.
   *
   * @param appId the app id
   * @param envId the env id
   * @param tagId the tag id
   * @param host  the host
   */
  void addApplicationHost(String appId, String envId, String tagId, Host host);

  /**
   * Exist boolean.
   *
   * @param appId  the app id
   * @param hostId the host id
   * @return the boolean
   */
  boolean exist(String appId, String hostId);
}
