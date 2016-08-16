package software.wings.app;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.handler.AtmosphereHandlerAdapter;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import software.wings.utils.JsonUtils;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 8/15/16.
 */
@AtmosphereHandlerService(path = "{channel}", interceptors = {AtmosphereResourceLifecycleInterceptor.class},
    broadcasterCache = UUIDBroadcasterCache.class)
public class UIStreamHandler extends AtmosphereHandlerAdapter {
  @Override
  public void onRequest(AtmosphereResource resource) throws IOException {
    System.out.println(resource);
  }

  @Override
  public void onStateChange(AtmosphereResourceEvent event) throws IOException {
    if (event.isSuspended()) {
      Object message = event.getMessage() == null ? null : event.getMessage();
      if (message != null) {
        event.getResource().write(JsonUtils.asJson(message));
      }
    }
  }
}
