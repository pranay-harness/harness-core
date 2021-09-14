// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package config

import (
	"github.com/kelseyhightower/envconfig"
)

// Config provides the system configuration.
type Config struct {
	Debug bool `envconfig:"TI_SERVICE_DEBUG"`
	Trace bool `envconfig:"TI_SERVICE_TRACE"`

	Secrets struct {
		LogSecret   string `envconfig:"TI_SERVICE_SECRET" default:"secret"`
		GlobalToken string `envconfig:"TI_SERVICE_GLOBAL_TOKEN" default:"token"`
		DisableAuth bool   `envconfig:"TI_SERVICE_DISABLE_AUTH"`
	}

	Server struct {
		Bind  string `envconfig:"TI_SERVICE_HTTP_BIND" default:":8078"`
		Proto string `envconfig:"TI_SERVICE_HTTP_PROTO"`
		Host  string `envconfig:"TI_SERVICE_HTTP_HOST"`
		Acme  bool   `envconfig:"TI_SERVICE_HTTP_ACME"`
	}

	TimeScaleDb struct {
		Username  string `envconfig:"TI_SERVICE_TIMESCALE_USERNAME"`
		Password  string `envconfig:"TI_SERVICE_TIMESCALE_PASSWORD"`
		Host      string `envconfig:"TI_SERVICE_TIMESCALE_HOST"`
		Port      string `envconfig:"TI_SERVICE_TIMESCALE_PORT"`
		EnableSSL bool   `envconfig:"TI_SERVICE_TIMESCALE_ENABLE_SSL"`
		// Path to root certificate provided by server in plaintext
		SSLCertPath    string `envconfig:"TI_SERVICE_TIMESCALE_SSL_CERT_PATH"`
		DbName         string `envconfig:"TI_SERVICE_DB_NAME"`
		EvalTable      string `envconfig:"TI_SERVICE_HYPER_TABLE"`
		SelectionTable string `envconfig:"TI_SERVICE_SELECTION_HYPER_TABLE"`
		CoverageTable  string `envconfig:"TI_SERVICE_COVERAGE_HYPER_TABLE"`
	}

	MongoDb struct {
		Username string `envconfig:"TI_SERVICE_MONGODB_USERNAME"`
		Password string `envconfig:"TI_SERVICE_MONGODB_PASSWORD"`
		Host     string `envconfig:"TI_SERVICE_MONGODB_HOST"`
		Port     string `envconfig:"TI_SERVICE_MONGODB_PORT"`
		DbName   string `envconfig:"TI_SERVICE_MONGODB_DB_NAME"`
		// Set this if you want to connect to mongoDB using a connection string directly
		// If this is not set, TI service will create a connStr using the host/port and
		// will try to create a direct connection to mongodb.
		ConnStr          string `envconfig:"TI_SERVICE_MONGODB_CONN_STR"`
		EnableReflection bool   `envconfig:"TI_SERVICE_MONGODB_ENABLE_REFLECTION"`
	}

	// Variables required to talk to redis as part of the events framework
	EventsFramework struct {
		RedisUrl      string `envconfig:"EVENTS_FRAMEWORK_REDIS_URL"`
		EnvNamespace  string `envconfig:"EVENTS_FRAMEWORK_ENV_NAMESPACE"`
		RedisPassword string `envconfig:"EVENTS_FRAMEWORK_REDIS_PASSWORD"`
		SSLEnabled    bool   `envconfig:"EVENTS_FRAMEWORK_REDIS_SSL_ENABLED"`
		// Path to b64 encoded .pem file
		CertPath string `envconfig:"EVENTS_FRAMEWORK_REDIS_SSL_CA_CERT_PATH"`
	}
}

// Load loads the configuration from the environment.
func Load() (Config, error) {
	cfg := Config{}
	err := envconfig.Process("", &cfg)
	return cfg, err
}
