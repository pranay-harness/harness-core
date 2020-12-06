package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.security.access.Whitelist;
import software.wings.service.intfc.ownership.OwnedByAccount;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author rktummala on 04/06/2018
 */
public interface WhitelistService extends OwnedByAccount {
  /**
   * Save.
   *
   * @param whitelist the whitelist config
   * @return the whitelist
   */
  Whitelist save(Whitelist whitelist);

  /**
   * List page response.
   *
   * @param req the req
   * @return the page response
   */
  /* (non-Javadoc)
   * @see software.wings.service.intfc.WhitelistService#list(software.wings.dl.PageRequest)
   */
  PageResponse<Whitelist> list(@NotEmpty String accountId, PageRequest<Whitelist> req);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @param accountId the accountId
   * @return the whitelist
   */
  Whitelist get(@NotEmpty String accountId, @NotEmpty String uuid);

  /**
   *
   * @param accountId
   * @param ipAddress
   * @return
   */
  boolean isValidIPAddress(@NotEmpty String accountId, @NotEmpty String ipAddress);

  Whitelist update(Whitelist whitelist);

  /**
   * Delete the given whitelist
   * @param accountId
   * @param whitelistId
   * @return
   */
  boolean delete(String accountId, String whitelistId);

  boolean deleteAll(String accountId);
}
