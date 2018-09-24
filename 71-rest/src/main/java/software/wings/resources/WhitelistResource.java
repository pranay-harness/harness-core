package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.security.access.Whitelist;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.WhitelistService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Whitelist resource class.
 *
 * @author rktummala on 03/29/18
 */
@Api("whitelist")
@Path("/whitelist")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.WHITE_LIST)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class WhitelistResource {
  private WhitelistService whitelistService;

  /**
   * Instantiates a new Access resource.
   *
   * @param whitelistService    the whitelist service
   */
  @Inject
  public WhitelistResource(WhitelistService whitelistService) {
    this.whitelistService = whitelistService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Whitelist>> list(
      @BeanParam PageRequest<Whitelist> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    PageResponse<Whitelist> pageResponse = whitelistService.list(accountId, pageRequest);
    return new RestResponse<>(pageResponse);
  }

  /**
   * Gets the whitelist config.
   *
   * @param accountId   the account id
   * @param whitelistId  the whitelistId
   * @return the rest response
   */
  @GET
  @Path("{whitelistId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Whitelist> get(
      @QueryParam("accountId") String accountId, @PathParam("whitelistId") String whitelistId) {
    return new RestResponse<>(whitelistService.get(accountId, whitelistId));
  }

  /**
   * Gets the whitelist config.
   *
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("isEnabled")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> isEnabled(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(whitelistService.isEnabled(accountId));
  }

  /**
   * Save.
   *
   * @param accountId   the account id
   * @param whitelist the whitelist
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Whitelist> save(@QueryParam("accountId") String accountId, Whitelist whitelist) {
    whitelist.setAccountId(accountId);
    return new RestResponse<>(whitelistService.save(whitelist));
  }

  /**
   * Update whitelist.
   *
   * @param accountId   the account id
   * @param whitelistId  the whitelistId
   * @param whitelist the whitelist
   * @return the rest response
   */
  @PUT
  @Path("{whitelistId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Whitelist> update(
      @QueryParam("accountId") String accountId, @PathParam("whitelistId") String whitelistId, Whitelist whitelist) {
    whitelist.setUuid(whitelistId);
    whitelist.setAccountId(accountId);
    return new RestResponse<>(whitelistService.update(whitelist));
  }

  /**
   * Delete.
   *
   * @param accountId   the account id
   * @param whitelistId  the whitelistId
   * @return the rest response
   */
  @DELETE
  @Path("{whitelistId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> delete(
      @QueryParam("accountId") String accountId, @PathParam("whitelistId") String whitelistId) {
    return new RestResponse<>(whitelistService.delete(accountId, whitelistId));
  }
}
