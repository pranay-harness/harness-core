package server

import (
	"github.com/kelseyhightower/envconfig"
)

// Config provides the system configuration.
type Config struct {
	Debug bool `envconfig:"LOG_SERVICE_DEBUG"`
	Trace bool `envconfig:"LOG_SERVICE_TRACE"`

	Server struct {
		Bind  string `envconfig:"LOG_SERVICE_HTTP_BIND" default:":8080"`
		Proto string `envconfig:"LOG_SERVICE_HTTP_PROTO"`
		Host  string `envconfig:"LOG_SERVICE_HTTP_HOST"`
		Acme  bool   `envconfig:"LOG_SERVICE_HTTP_ACME"`
	}

	Bolt struct {
		Path string `envconfig:"LOG_SERVICE_BOLT_PATH" default:"bolt.db"`
	}

	Minio struct {
		Bucket    string `envconfig:"LOG_SERVICE_MINIO_BUCKET"`
		Prefix    string `envconfig:"LOG_SERVICE_MINIO_PREFIX"`
		Endpoint  string `envconfig:"LOG_SERVICE_MINIO_ENDPOINT"`
		PathStyle bool   `envconfig:"LOG_SERVICE_MINIO_PATH_STYLE"`
	}
}

// Load loads the configuration from the environment.
func Load() (Config, error) {
	cfg := Config{}
	err := envconfig.Process("", &cfg)
	return cfg, err
}
