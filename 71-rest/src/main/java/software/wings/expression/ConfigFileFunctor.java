package software.wings.expression;

import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.FunctorException;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;

import java.nio.charset.Charset;

@Builder
public class ConfigFileFunctor implements ExpressionFunctor {
  private static final Logger logger = LoggerFactory.getLogger(ConfigFileFunctor.class);

  private ServiceTemplateService serviceTemplateService;
  private ConfigService configService;
  private String appId;
  private String envId;
  private String serviceTemplateId;

  public Object getAsString(String relativeFilePath) {
    if (serviceTemplateId == null) {
      throw new FunctorException("Cannot evaluate expression: ${configFile.getAsString(\"" + relativeFilePath + "\")}");
    }
    byte[] fileContent = getConfigFileContent(relativeFilePath);
    return new String(fileContent, Charset.forName("UTF-8"));
  }

  public Object getAsBase64(String relativeFilePath) {
    if (serviceTemplateId == null) {
      throw new FunctorException("Cannot evaluate expression: ${configFile.getAsBase64(\"" + relativeFilePath + "\")}");
    }
    byte[] fileContent = getConfigFileContent(relativeFilePath);
    return EncodingUtils.encodeBase64(fileContent);
  }

  private byte[] getConfigFileContent(String relativeFilePath) {
    logger.info("Get content for file: {}", relativeFilePath);
    ConfigFile configFile =
        serviceTemplateService.computedConfigFileByRelativeFilePath(appId, envId, serviceTemplateId, relativeFilePath);
    if (configFile == null) {
      throw new FunctorException("Config file " + relativeFilePath + " not found");
    }

    logger.info("ConfigFile details: relativePath:{}, encrypted:{}, encryptedFileId:{}, fileId:{}",
        configFile.getRelativeFilePath(), configFile.isEncrypted(), configFile.getEncryptedFileId(),
        configFile.getUuid());

    return configService.getFileContent(appId, configFile);
  }
}
