package io.harness.pms.pipeline.service.yamlschema;

import io.harness.ModuleType;
import io.harness.pms.pipeline.service.yamlschema.approval.ApprovalYamlSchemaService;
import io.harness.pms.pipeline.service.yamlschema.featureflag.FeatureFlagYamlService;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.client.YamlSchemaClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.NonNull;

@Singleton
public class SchemaGetterFactory {
  private final ApprovalYamlSchemaService approvalYamlSchemaService;
  private final FeatureFlagYamlService featureFlagYamlService;
  private final YamlSchemaProvider yamlSchemaProvider;
  private Map<String, YamlSchemaClient> yamlSchemaClientMapper;
  private final PmsYamlSchemaHelper pmsYamlSchemaHelper;

  @Inject
  public SchemaGetterFactory(ApprovalYamlSchemaService approvalYamlSchemaService,
      FeatureFlagYamlService featureFlagYamlService, YamlSchemaProvider yamlSchemaProvider,
      Map<String, YamlSchemaClient> yamlSchemaClientMapper, PmsYamlSchemaHelper pmsYamlSchemaHelper) {
    this.approvalYamlSchemaService = approvalYamlSchemaService;
    this.featureFlagYamlService = featureFlagYamlService;
    this.yamlSchemaProvider = yamlSchemaProvider;
    this.yamlSchemaClientMapper = yamlSchemaClientMapper;
    this.pmsYamlSchemaHelper = pmsYamlSchemaHelper;
  }
  public SchemaGetter obtainGetter(@NonNull String accountIdentifier, @NonNull ModuleType moduleType) {
    if (moduleType == ModuleType.PMS) {
      return new LocalSchemaGetter(accountIdentifier, moduleType, yamlSchemaProvider, approvalYamlSchemaService,
          featureFlagYamlService, pmsYamlSchemaHelper);
    }

    YamlSchemaClient yamlSchemaClient = yamlSchemaClientMapper.get(moduleType.name().toLowerCase());
    if (yamlSchemaClient == null) {
      throw new IllegalStateException();
    }
    return new RemoteSchemaGetter(yamlSchemaClient, moduleType, accountIdentifier);
  }
}
