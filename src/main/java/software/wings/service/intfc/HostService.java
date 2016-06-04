package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.util.List;

// TODO: Auto-generated Javadoc

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
  public PageResponse<Host> list(PageRequest<Host> req);

  /**
   * Gets the.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param hostId  the host id
   * @return the host
   */
  public Host get(String appId, String infraId, String hostId);

  /**
   * Save.
   *
   * @param host the host
   * @return the host
   */
  public Host save(Host host);

  /**
   * Update.
   *
   * @param host the host
   * @return the host
   */
  public Host update(Host host);

  /**
   * Export hosts.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @return the file
   */
  File exportHosts(String appId, String infraId);

  /**
   * Gets the infra id.
   *
   * @param envId the env id
   * @param appId the app id
   * @return the infra id
   */
  String getInfraId(String envId, String appId);

  /**
   * Delete.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param hostId  the host id
   */
  void delete(String appId, String infraId, String hostId);

  /**
   * Bulk save.
   *
   * @param baseHost  the base host
   * @param hostNames the host names
   */
  void bulkSave(Host baseHost, List<String> hostNames);

  /**
   * Import hosts.
   *
   * @param appId              the app id
   * @param infraId            the infra id
   * @param boundedInputStream the bounded input stream
   * @return the int
   */
  int importHosts(String appId, String infraId, BoundedInputStream boundedInputStream);

  /**
   * Gets the hosts by id.
   *
   * @param appId     the app id
   * @param hostUuids the host uuids
   * @return the hosts by id
   */
  List<Host> getHostsById(String appId, List<String> hostUuids);

  /**
   * Gets the hosts by tags.
   *
   * @param appId the app id
   * @param tags  the tags
   * @return the hosts by tags
   */
  List<Host> getHostsByTags(String appId, List<Tag> tags);
}
