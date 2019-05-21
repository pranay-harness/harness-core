package software.wings.service.impl.analysis;

import static software.wings.utils.Misc.generateSecretKey;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceSecretKeyKeys;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.LearningEngineService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rsingh on 1/9/18.
 */
@Singleton
@Slf4j
public class LearningEngineAnalysisServiceImpl implements LearningEngineService {
  private static final String SERVICE_VERSION_FILE = "/service_version.properties";

  @Inject private WingsPersistence wingsPersistence;
  private final ServiceApiVersion learningEngineApiVersion;

  public LearningEngineAnalysisServiceImpl() throws IOException {
    Properties messages = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(SERVICE_VERSION_FILE);
      messages.load(in);
    } finally {
      if (in != null) {
        in.close();
      }
    }
    String apiVersion = messages.getProperty(ServiceType.LEARNING_ENGINE.name());
    Preconditions.checkState(!StringUtils.isEmpty(apiVersion));
    learningEngineApiVersion = ServiceApiVersion.valueOf(apiVersion.toUpperCase());
  }

  @Override
  public void initializeServiceSecretKeys() {
    for (ServiceType serviceType : ServiceType.values()) {
      wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(
          ServiceSecretKey.builder().serviceType(serviceType).serviceSecret(generateSecretKey()).build()));
    }
  }

  @Override
  public String getServiceSecretKey(ServiceType serviceType) {
    Preconditions.checkNotNull(serviceType);
    return wingsPersistence.createQuery(ServiceSecretKey.class)
        .filter(ServiceSecretKeyKeys.serviceType, serviceType)
        .get()
        .getServiceSecret();
  }
}
