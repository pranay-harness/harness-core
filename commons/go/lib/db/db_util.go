package db

import (
	"fmt"
	"github.com/kamva/mgm/v3"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

const DEFAULT_TIMEOUT = 15

func getDefaultConfig() *mgm.Config {
	// TODO: (vistaar) Decrease this to a reasonable value (1 second or so).
	// Right now queries are slow while running locally.
	return &mgm.Config{
		CtxTimeout: DEFAULT_TIMEOUT * time.Second,
	}
}

func ConfigureMongoClient(username string, password string, host string, port string, connStr string) *options.ClientOptions {
	// If any connStr is provided, use that
	if connStr == "" {
		connStr = fmt.Sprintf("mongodb://%s:%s/?connect=direct", host, port)
	}
	opts := options.Client().ApplyURI(connStr)
	if len(username) > 0 {
		credential := options.Credential{
			Username: username,
			Password: password,
		}
		opts = opts.SetAuth(credential)
	}
	return opts
}

func ConfigureMgm(username, password, host, port, dbName string, connStr string) error {
	opts := ConfigureMongoClient(username, password, host, port, connStr)
	return mgm.SetDefaultConfig(getDefaultConfig(), dbName, opts)
}
