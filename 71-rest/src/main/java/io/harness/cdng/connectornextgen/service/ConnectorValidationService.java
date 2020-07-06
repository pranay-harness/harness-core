package io.harness.cdng.connectornextgen.service;

import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;

public interface ConnectorValidationService { boolean validate(ConnectorRequestDTO connector, String accountId); }
