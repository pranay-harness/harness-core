/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.stream;

import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.eraro.Level.ERROR;
import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.MessageManager;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;

import software.wings.beans.AuthToken;
import software.wings.security.PermissionAttribute;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AuthService;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@AtmosphereHandlerService(path = "/stream/ui/{channel}", interceptors = {AtmosphereResourceLifecycleInterceptor.class},
    broadcasterCache = UUIDBroadcasterCache.class)
@Slf4j
public class UiStreamHandler extends AtmosphereHandlerAdapter {
  public static final Splitter SPLITTER = Splitter.on("/").omitEmptyStrings();
  @Inject private AuthService authService;

  @Override
  public void onRequest(AtmosphereResource resource) throws IOException {
    AtmosphereRequest req = resource.getRequest();
    log.info("Received request with  Broadcaster Id {}", resource.getBroadcaster().getID());
    if (req.getMethod().equals("GET")) {
      if (isBlank(req.getParameter("token"))) {
        sendError(resource, INVALID_TOKEN);
        return;
      }

      try {
        log.info("Verifying Delegate token");
        AuthToken authToken = authService.validateToken(req.getParameter("token"));
        log.info("Delegate token verified");

        List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
        if (pathSegments.size() <= 5) {
          sendError(resource, INVALID_REQUEST, "message", "Request had too few path segments");
        }

        String accountId = pathSegments.get(1);
        String appId = pathSegments.get(2).replace("all", GLOBAL_APP_ID);
        String envId = pathSegments.get(3).replace("all", GLOBAL_ENV_ID);
        Channel channel = Channel.getChannelByChannelName(pathSegments.get(5));

        if (channel == null) {
          sendError(resource, INVALID_REQUEST, "message", "Channel was null");
        }
        PermissionAttribute permissionAttribute =
            new PermissionAttribute(channel.getPermission(), channel.getScope(), "GET");
        log.info("Verifying authorization");
        authService.authorize(accountId, appId, envId, authToken.getUser(), asList(permissionAttribute), null);
        log.info("Authorization successful");

      } catch (WingsException e) {
        sendError(resource, e.getCode(), e.getParams());
        return;
      } catch (Exception e) {
        sendError(resource, UNKNOWN_ERROR);
        return;
      }

      resource.suspend();
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      String line = req.getReader().readLine();
      if (line != null) {
        resource.getBroadcaster().broadcast(line.trim());
      }
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent event) throws IOException {
    AtmosphereResource r = event.getResource();
    AtmosphereResponse res = r.getResponse();

    if (r.isSuspended()) {
      if (event.getMessage() != null) {
        event.getResource().write(JsonUtils.asJson(event.getMessage()));
      }

      AtmosphereResource.TRANSPORT transport = r.transport();
      switch (transport) {
        case JSONP:
        case LONG_POLLING:
          event.getResource().resume();
          break;
        case WEBSOCKET:
          break;
        case STREAMING:
          res.getWriter().flush();
          break;
        default:
          unhandled(transport);
      }
    }
  }

  private void sendError(AtmosphereResource resource, ErrorCode errorCode) throws IOException {
    sendError(resource, errorCode, null, null);
  }

  private void sendError(AtmosphereResource resource, ErrorCode errorCode, String paramKey, String paramValue)
      throws IOException {
    Map<String, Object> params = new HashMap<>();
    if (isNotBlank(paramKey) && paramValue != null) {
      params.put(paramKey, paramValue);
    }

    sendError(resource, errorCode, params);
  }

  private void sendError(AtmosphereResource resource, ErrorCode errorCode, Map<String, Object> params)
      throws IOException {
    AtmosphereResource.TRANSPORT transport = resource.transport();
    switch (transport) {
      case JSONP:
      case LONG_POLLING:
        resource.getResponse().sendError(errorCode.getStatus().getCode());
        break;
      case WEBSOCKET:
        break;
      case STREAMING:
        break;
      default:
        unhandled(transport);
    }
    resource.write(JsonUtils.asJson(ResponseMessage.builder()
                                        .code(errorCode)
                                        .level(ERROR)
                                        .message(MessageManager.getInstance().prepareMessage(
                                            ErrorCodeName.builder().value(errorCode.name()).build(), null, params))
                                        .build()));
    resource.close();
  }
}
