package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Host;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.validation.Update;

import java.io.File;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
  public Host get(@NotEmpty String appId, @NotEmpty String infraId, @NotEmpty String hostId);

  /**
   * Save.
   *
   * @param host the host
   * @return the host
   */
  public Host save(@Valid Host host);

  /**
   * Update.
   *
   * @param host the host
   * @return the host
   */
  @ValidationGroups(Update.class) public Host update(@Valid Host host);

  /**
   * Export hosts.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @return the file
   */
  File exportHosts(@NotEmpty String appId, @NotEmpty String infraId);

  /**
   * Gets the infra id.
   *
   * @param envId the env id
   * @param appId the app id
   * @return the infra id
   */
  String getInfraId(@NotEmpty String envId, @NotEmpty String appId);

  /**
   * Delete.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param hostId  the host id
   */
  void delete(@NotEmpty String appId, @NotEmpty String infraId, @NotEmpty String hostId);

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
  int importHosts(@NotEmpty String appId, @NotEmpty String infraId, @NotNull BoundedInputStream boundedInputStream);

  /**
   * Gets the hosts by id.
   *
   * @param appId     the app id
   * @param hostUuids the host uuids
   * @return the hosts by id
   */
  List<Host> getHostsById(@NotEmpty String appId, @NotNull List<String> hostUuids);

  /**
   * Gets the hosts by tags.
   *
   * @param appId the app id
   * @param tags  the tags
   * @return the hosts by tags
   */
  List<Host> getHostsByTags(@NotEmpty String appId, @NotEmpty String envId, @NotNull List<Tag> tags);
}
