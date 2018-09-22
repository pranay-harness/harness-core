package software.wings.utils;

import static org.apache.commons.lang3.StringUtils.removeEnd;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 6/17/16.
 */
@SwaggerDefinition
public class SwaggerFileExamplesReader implements ReaderListener {
  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
  }

  private static final Logger logger = LoggerFactory.getLogger(SwaggerFileExamplesReader.class);

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {
    Info info = new Info();
    info.title("Harness Manager");
    swagger.info(info);

    swagger.securityDefinition("oauth", new OAuth2Definition().password("/users/login"));
  }

  @Override
  public void afterScan(Reader reader, Swagger swagger) {
    for (Map.Entry<String, Path> entry : swagger.getPaths().entrySet()) {
      String path = entry.getKey();
      List<Operation> operations = entry.getValue().getOperations();
      for (Operation operation : operations) {
        Optional<Parameter> bodyParameter =
            operation.getParameters().stream().filter(parameter -> parameter instanceof BodyParameter).findFirst();
        if (bodyParameter.isPresent()) {
          String exampleFileName = removeEnd(path.replace("/", "").replace("{", ".").replace("}", "."), ".") + "."
              + operation.getOperationId() + ".json";

          Object resource = null;
          try {
            resource = JsonUtils.readResource("/apiexamples/" + exampleFileName);
          } catch (RuntimeException ignore) {
            // no example file
            continue;
          }
          String json = JsonUtils.asJson(resource, mapper);
          ((BodyParameter) bodyParameter.get()).setExamples(ImmutableMap.of("default", json));
        }
      }
    }
  }
}
