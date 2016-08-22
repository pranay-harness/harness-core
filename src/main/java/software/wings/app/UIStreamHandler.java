package software.wings.app;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ErrorCodes.INVALID_TOKEN;
import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import software.wings.beans.AuthToken;
import software.wings.beans.ErrorCodes;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AuthService;
import software.wings.utils.JsonUtils;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@AtmosphereHandlerService(path = "{channel}", interceptors = {AtmosphereResourceLifecycleInterceptor.class},
    broadcasterCache = UUIDBroadcasterCache.class)
public class UIStreamHandler extends AtmosphereHandlerAdapter {
  @Inject private AuthService authService;

  @Override
  public void onRequest(AtmosphereResource resource) throws IOException {
    AtmosphereRequest req = resource.getRequest();
    if (req.getMethod().equals("GET")) {
      if (isBlank(req.getParameter("token"))) {
        sendError(resource, INVALID_TOKEN);
        return;
      }

      try {
        AuthToken authToken = authService.validateToken(req.getParameter("token"));

        String appId = req.getPathInfo().split("/")[2];
        String envId = req.getPathInfo().split("/")[3];
        authService.authorize(appId, envId, authToken.getUser(), Lists.newArrayList());
      } catch (WingsException e) {
        sendError(resource, e.getResponseMessageList().get(0).getCode());
        return;
      } catch (Exception e) {
        sendError(resource, UNKNOWN_ERROR);
        return;
      }

      resource.suspend();
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      resource.getBroadcaster().broadcast(req.getReader().readLine().trim());
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent event) throws IOException {
    AtmosphereResource r = event.getResource();
    AtmosphereResponse res = r.getResponse();

    if (r.isSuspended()) {
      Object message = event.getMessage() == null ? null : event.getMessage();
      if (message != null) {
        event.getResource().write(JsonUtils.asJson(message));
      }
      switch (r.transport()) {
        case JSONP:
        case LONG_POLLING:
          event.getResource().resume();
          break;
        case WEBSOCKET:
          break;
        case STREAMING:
          res.getWriter().flush();
          break;
      }
    }
  }

  private void sendError(AtmosphereResource resource, ErrorCodes errorCodes) throws IOException {
    switch (resource.transport()) {
      case JSONP:
      case LONG_POLLING:
        resource.getResponse().sendError(errorCodes.getStatus().getStatusCode());
        break;
      case WEBSOCKET:
        break;
      case STREAMING:
        break;
    }
    resource.write(JsonUtils.asJson(ResponseCodeCache.getInstance().getResponseMessage(errorCodes)));
    resource.close();
  }
}
