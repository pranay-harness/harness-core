package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.dmsclient.DelegateAgentDmsClient;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.TextFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@OwnedBy(HarnessTeam.DEL)
public class DelegatePropertyServiceImpl implements DelegatePropertyService {
  // TODO: add variable expiration time according to key
  private final LoadingCache<GetDelegatePropertiesRequest, GetDelegatePropertiesResponse> delegatePropertyCache =

      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .expireAfterWrite(300, TimeUnit.MINUTES)
          .build(new CacheLoader<GetDelegatePropertiesRequest, GetDelegatePropertiesResponse>() {
            @Override
            @SneakyThrows
            public GetDelegatePropertiesResponse load(GetDelegatePropertiesRequest request) {
              return TextFormat.parse(
                  execute(delegateAgentDmsClient.getDelegateProperties(request.getAccountId(),
                              RequestBody.create(MediaType.parse("application/octet-stream"), request.toByteArray())))
                      .getResource(),
                  GetDelegatePropertiesResponse.class);
            }
          });

  private final DelegateAgentDmsClient delegateAgentDmsClient;

  @Inject
  public DelegatePropertyServiceImpl(DelegateAgentDmsClient delegateAgentDmsClient) {
    this.delegateAgentDmsClient = delegateAgentDmsClient;
  }

  @Override
  public GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request)
      throws ExecutionException {
    return delegatePropertyCache.get(request);
  }

  @Override
  public void resetCache() {
    delegatePropertyCache.invalidateAll();
  }
}
