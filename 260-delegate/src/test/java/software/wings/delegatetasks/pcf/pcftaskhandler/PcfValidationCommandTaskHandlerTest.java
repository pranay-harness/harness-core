package software.wings.delegatetasks.pcf.pcftaskhandler;

import static io.harness.rule.OwnerRule.ANSHUL;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.PcfDeploymentManager;
import software.wings.helpers.ext.pcf.request.PcfInfraMappingDataRequest;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PcfValidationCommandTaskHandlerTest extends WingsBaseTest {
  @Mock private PcfDeploymentManager pcfDeploymentManager;
  @Mock private EncryptionService encryptionService;

  @InjectMocks @Inject private PcfValidationCommandTaskHandler pcfValidationCommandTaskHandler;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void TestDecryptionOfPcfConfig() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    PcfConfig pcfConfig = PcfConfig.builder().accountId(ACCOUNT_ID).password("password".toCharArray()).build();

    pcfValidationCommandTaskHandler.executeTaskInternal(
        PcfInfraMappingDataRequest.builder().pcfConfig(pcfConfig).build(), encryptedDataDetails, null, false);

    verify(encryptionService).decrypt(pcfConfig, encryptedDataDetails, false);
  }
}
