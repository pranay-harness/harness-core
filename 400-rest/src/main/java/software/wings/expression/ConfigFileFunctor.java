package software.wings.expression;

import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.FunctorException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.ExpressionFunctor;

import software.wings.beans.ConfigFile;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceTemplateService;

import java.nio.charset.Charset;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class ConfigFileFunctor implements ExpressionFunctor {
  // Some customers have large JSON files with size around 512K, so MAX_CONFIG_FILE_SIZE should be greater than that.
  static final int MAX_CONFIG_FILE_SIZE = 4 * ExpressionEvaluatorUtils.EXPANSION_LIMIT;

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
    log.info("Get content for file: {}", relativeFilePath);
    ConfigFile configFile =
        serviceTemplateService.computedConfigFileByRelativeFilePath(appId, envId, serviceTemplateId, relativeFilePath);
    if (configFile == null) {
      throw new FunctorException("Config file " + relativeFilePath + " not found");
    }

    log.info("ConfigFile details: relativePath:{}, encrypted:{}, encryptedFileId:{}, fileId:{}",
        configFile.getRelativeFilePath(), configFile.isEncrypted(), configFile.getEncryptedFileId(),
        configFile.getUuid());

    byte[] contents = configService.getFileContent(appId, configFile);
    if (contents.length > MAX_CONFIG_FILE_SIZE) {
      throw new FunctorException("Too large config file " + relativeFilePath);
    }
    return contents;
  }
}
