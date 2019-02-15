package software.wings.resources;

import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.configuration.ConfigurationType;
import io.harness.exception.WingsException;
import io.harness.mongo.MongoConfig;
import io.harness.rest.RestResponse;
import io.harness.security.AsymmetricEncryptor;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.security.annotations.PublicApi;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by brett on 12/3/17
 */
@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
public class HealthResource {
  private static final Logger logger = LoggerFactory.getLogger(HealthResource.class);

  private MainConfiguration mainConfiguration;
  private AsymmetricEncryptor asymmetricEncryptor;

  @Inject
  public HealthResource(MainConfiguration mainConfiguration, AsymmetricEncryptor asymmetricEncryptor) {
    this.mainConfiguration = mainConfiguration;
    this.asymmetricEncryptor = asymmetricEncryptor;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> get() {
    if (isMaintenance()) {
      logger.info("In maintenance mode. Throwing exception to prevent traffic.");
      throw new WingsException(RESOURCE_NOT_FOUND, USER);
    }
    return new RestResponse<>("healthy");
  }

  @GET
  @Path("configuration")
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getConfiguraton(@QueryParam("configurationType") ConfigurationType configurationType)
      throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException,
             InvalidKeyException {
    // The following code can be refactored to move to service layer
    switch (configurationType) {
      case MONGO:
        MongoConfig mongoConfig = mainConfiguration.getMongoConnectionFactory();
        return new RestResponse<>(MongoConfig.builder()
                                      .encryptedUri(asymmetricEncryptor.encryptText(mongoConfig.getUri()))
                                      .encryptedLocksUri(asymmetricEncryptor.encryptText(
                                          mongoConfig.getLocksUri() != null ? mongoConfig.getLocksUri() : ""))
                                      .build());
      default:
        unhandled(configurationType);
    }

    return new RestResponse<>();
  }
}
