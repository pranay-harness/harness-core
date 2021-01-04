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
	}

	Server struct {
		Bind  string `envconfig:"TI_SERVICE_HTTP_BIND" default:":8078"`
		Proto string `envconfig:"TI_SERVICE_HTTP_PROTO"`
		Host  string `envconfig:"TI_SERVICE_HTTP_HOST"`
		Acme  bool   `envconfig:"TI_SERVICE_HTTP_ACME"`
	}

	TimeScaleDb struct {
		Username       string `envconfig:"TI_SERVICE_TIMESCALE_USERNAME"`
		Password       string `envconfig:"TI_SERVICE_TIMESCALE_PASSWORD"`
		Host           string `envconfig:"TI_SERVICE_TIMESCALE_HOST"`
		Port           string `envconfig:"TI_SERVICE_TIMESCALE_PORT"`
		DbName         string `envconfig:"TI_SERVICE_DB_NAME"`
		HyperTableName string `envconfig:"TI_SERVICE_HYPER_TABLE"`
	}
}

// Load loads the configuration from the environment.
func Load() (Config, error) {
	cfg := Config{}
	err := envconfig.Process("", &cfg)
	return cfg, err
}
