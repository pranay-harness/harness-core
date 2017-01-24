package software.wings.app;

import static software.wings.beans.ErrorCodes.UNKNOWN_ERROR;

import com.google.common.base.Splitter;
import com.google.inject.Inject;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.WebSocketHandlerService;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketHandlerAdapter;
import org.atmosphere.websocket.WebSocketProcessor.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.ErrorCodes;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@WebSocketHandlerService(path = "/stream/delegate/{accountId}",
    interceptors = {AtmosphereResourceLifecycleInterceptor.class}, broadcasterCache = UUIDBroadcasterCache.class,
    broadcastFilters = {KryoBroadcastFilter.class})
public class DelegateStreamHandler extends WebSocketHandlerAdapter {
  public static final Splitter SPLITTER = Splitter.on("/").omitEmptyStrings();
  private static final Logger logger = LoggerFactory.getLogger(DelegateStreamHandler.class);
  @Inject private AuthService authService;
  @Inject private DelegateService delegateService;

  /** {@inheritDoc} */
  @Override
  public void onOpen(WebSocket webSocket) throws IOException {
    AtmosphereRequest req = webSocket.resource().getRequest();
    try {
      List<String> pathSegments = SPLITTER.splitToList(req.getPathInfo());
      String accountId = pathSegments.get(1);
      authService.validateDelegateToken(accountId, req.getParameter("token"));

      String delegateId = req.getParameter("delegateId");

      Delegate delegate = delegateService.get(accountId, delegateId);
      delegate.setStatus(Status.ENABLED);
      delegate.setConnected(true);
      delegateService.update(delegate);
      delegateService.update(delegate);
    } catch (WingsException e) {
      sendError(webSocket, e.getResponseMessageList().get(0).getCode());
      return;
    } catch (Exception e) {
      sendError(webSocket, UNKNOWN_ERROR);
      return;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void onByteMessage(WebSocket webSocket, byte[] data, int offset, int length) throws IOException {
    super.onByteMessage(webSocket, data, offset, length);
  }

  /** {@inheritDoc} */
  @Override
  public void onTextMessage(WebSocket webSocket, String data) throws IOException {
    delegateService.update(JsonUtils.asObject(data, Delegate.class));
  }

  /** {@inheritDoc} */
  @Override
  public void onClose(WebSocket webSocket) {
    AtmosphereRequest request = webSocket.resource().getRequest();
    List<String> pathSegments = SPLITTER.splitToList(request.getPathInfo());
    String delegateId = request.getParameter("delegateId");
    String accountId = pathSegments.get(1);
    Delegate delegate = delegateService.get(accountId, delegateId);
    delegate.setConnected(false);
    delegateService.update(delegate);
    logger.info("Connection Closed");
    super.onClose(webSocket);
  }

  /** {@inheritDoc} */
  @Override
  public void onError(WebSocket webSocket, WebSocketException t) {
    super.onError(webSocket, t);
  }

  private void sendError(WebSocket webSocket, ErrorCodes errorCodes) throws IOException {
    webSocket.write(JsonUtils.asJson(ResponseCodeCache.getInstance().getResponseMessage(errorCodes)));
    webSocket.close();
  }
}
