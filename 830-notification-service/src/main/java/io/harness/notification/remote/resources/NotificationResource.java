/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.notification.remote.mappers.NotificationMapper.toDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.Team;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.dtos.NotificationDTO;
import io.harness.notification.entities.Notification;
import io.harness.notification.service.api.NotificationService;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Optional;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
@Api("notifications")
@Path("notifications")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class NotificationResource {
  private final NotificationService notificationService;
  @GET
  @Path("/{id}")
  @ApiOperation(value = "Get details of a notification", nickname = "getNotification")
  public ResponseDTO<NotificationDTO> get(@PathParam("id") String id) {
    Optional<Notification> notificationOptional = notificationService.getnotification(id);
    return ResponseDTO.newResponse(toDTO(notificationOptional.orElse(null)).orElse(null));
  }

  @GET
  @ApiOperation(value = "List notifications", nickname = "listNotifications")
  public ResponseDTO<PageResponse<NotificationDTO>> list(
      @QueryParam("team") Team team, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField("lastModifiedAt", SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Page<NotificationDTO> results = notificationService.list(team, pageRequest).map(x -> toDTO(x).orElse(null));
    return ResponseDTO.newResponse(getNGPageResponse(results));
  }
}
