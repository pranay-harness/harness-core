package io.harness.cdng.connectornextgen.service;

import io.harness.connector.apis.dto.ConnectorRequestDTO;

public interface ConnectorValidationService { boolean validate(ConnectorRequestDTO connector, String accountId); }
