package software.wings.service.impl.ldap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.harness.threading.ThreadPool;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/***
 * This is singleton class for re-using the executor service
 * for ldap based queries.
 *
 * Ideally, I want to mark the class as final but I am not sure
 * whether lombok will work correctly with it. Will test it.
 */
@FieldNameConstants(innerTypeName = "LdapExecutorServiceKeys")
public class LdapExecutorService {
  private static final LdapExecutorService INSTANCE = new LdapExecutorService();

  @Getter(lazy = true) private final ExecutorService executorService = createExecutorService();

  private LdapExecutorService() {}

  public static LdapExecutorService getInstance() {
    return INSTANCE;
  }

  private ExecutorService createExecutorService() {
    int processorsCount = Runtime.getRuntime().availableProcessors();
    int maxPoolSize = 2 * processorsCount;
    ExecutorService threadPoolExecutor = ThreadPool.create(processorsCount, maxPoolSize, 1, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("ldap-search-task-%d").setPriority(Thread.NORM_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { threadPoolExecutor.shutdownNow(); }));
    return threadPoolExecutor;
  }
}
