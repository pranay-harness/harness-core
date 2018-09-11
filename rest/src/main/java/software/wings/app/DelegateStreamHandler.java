package software.wings.app;

import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.Level;
import io.harness.eraro.MessageManager;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@AtmosphereHandlerService(path = "/stream/delegate/{accountId}",
    interceptors = {AtmosphereResourceLifecycleInterceptor.class}, broadcasterCache = UUIDBroadcasterCache.class,
    broadcastFilters = {DelegateEventFilter.class})
public class DelegateStreamHandler extends AtmosphereHandlerAdapter {
  public static final Splitter SPLITTER = Splitter.on("/").omitEmptyStrings();
  private static final Logger logger = LoggerFactory.getLogger(DelegateStreamHandler.class);

  @Inject private AuthService authService;
  @Inject private DelegateService delegateService;

  @Override
  public void onRequest(AtmosphereResource resource) throws IOException {
    AtmosphereRequest req = resource.getRequest();

    if (req.getMethod().equals("GET")) {
      try {
        List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
        String accountId = pathSegments.get(1);
        authService.validateDelegateToken(accountId, req.getParameter("token"));

        String delegateId = req.getParameter("delegateId");
        String delegateConnectionId = req.getParameter("delegateConnectionId");
        String delegateVersion = req.getHeader("Version");

        Delegate delegate = delegateService.get(accountId, delegateId);
        delegate.setStatus(Status.ENABLED);
        delegate.setConnected(true);
        delegateService.register(delegate);
        delegateService.doConnectionHeartbeat(accountId, delegateId,
            DelegateConnectionHeartbeat.builder()
                .delegateConnectionId(delegateConnectionId)
                .version(delegateVersion)
                .alive(true)
                .build());

        resource.addEventListener(new AtmosphereResourceEventListenerAdapter() {
          @Override
          public void onDisconnect(AtmosphereResourceEvent event) {
            Delegate delegate = delegateService.get(accountId, delegateId);
            delegate.setConnected(false);
            delegateService.register(delegate);
            delegateService.removeDelegateConnection(accountId, delegateConnectionId);
          }
        });
      } catch (WingsException e) {
        sendError(resource, e.getCode());
        return;
      } catch (Exception e) {
        sendError(resource, UNKNOWN_ERROR);
        return;
      }
      resource.suspend();
    } else if (req.getMethod().equalsIgnoreCase("POST")) {
      List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
      String accountId = pathSegments.get(1);
      String delegateId = req.getParameter("delegateId");
      String delegateConnectionId = req.getParameter("delegateConnectionId");
      String delegateVersion = req.getHeader("Version");

      Delegate delegate = JsonUtils.asObject(CharStreams.toString(req.getReader()), Delegate.class);
      if (delegate.getAppId() == null) {
        delegate.setAppId(Base.GLOBAL_APP_ID);
      }
      delegateService.register(delegate);
      delegateService.doConnectionHeartbeat(accountId, delegateId,
          DelegateConnectionHeartbeat.builder()
              .delegateConnectionId(delegateConnectionId)
              .version(delegateVersion)
              .alive(true)
              .build());
    }
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent event) throws IOException {
    AtmosphereResource r = event.getResource();
    AtmosphereResponse res = r.getResponse();

    if (r.isSuspended()) {
      Object message = event.getMessage();
      if (message != null) {
        if (message instanceof String) {
          event.getResource().write((String) message);
        } else {
          event.getResource().write(JsonUtils.asJson(message));
        }
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
    AtmosphereResource.TRANSPORT transport = resource.transport();
    switch (resource.transport()) {
      case JSONP:
      case LONG_POLLING:
        resource.getResponse().sendError(errorCode.getStatus().getStatusCode());
        break;
      case WEBSOCKET:
        break;
      case STREAMING:
        break;
      default:
        unhandled(transport);
    }
    resource.write(JsonUtils.asJson(aResponseMessage()
                                        .code(errorCode)
                                        .level(Level.ERROR)
                                        .message(MessageManager.getInstance().prepareMessage(
                                            ErrorCodeName.builder().value(errorCode.name()).build(), null, null))
                                        .build()));
    resource.close();
  }
}
