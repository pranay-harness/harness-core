package io.harness.delegate.task.azure.appservice.webapp.response;

import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AzureWebAppListWebAppNamesResponse implements AzureAppServiceTaskResponse {
  private List<String> webAppNames;
}
