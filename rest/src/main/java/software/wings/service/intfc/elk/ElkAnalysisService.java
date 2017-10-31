package software.wings.service.intfc.elk;

import software.wings.beans.ElkConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.intfc.analysis.AnalysisService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 8/23/17.
 */
public interface ElkAnalysisService extends AnalysisService {
  Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException;
  String getVersion(String accountId, ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException;
}
