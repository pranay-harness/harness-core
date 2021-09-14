// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package server

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/signal"

	"github.com/wings-software/portal/product/log-service/config"
	"github.com/wings-software/portal/product/log-service/handler"
	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/server"
	"github.com/wings-software/portal/product/log-service/store"
	"github.com/wings-software/portal/product/log-service/store/bolt"
	"github.com/wings-software/portal/product/log-service/store/s3"
	"github.com/wings-software/portal/product/log-service/stream"
	"github.com/wings-software/portal/product/log-service/stream/memory"
	"github.com/wings-software/portal/product/log-service/stream/redis"

	"github.com/joho/godotenv"
	"github.com/sirupsen/logrus"
	"gopkg.in/alecthomas/kingpin.v2"
)

type serverCommand struct {
	envfile string
}

func (c *serverCommand) run(*kingpin.ParseContext) error {
	godotenv.Load(c.envfile)

	// load the system configuration from the environment.
	config, err := config.Load()
	if err != nil {
		logrus.WithError(err).
			Errorln("cannot load the service configuration")
		return err
	}

	// init the system logging.
	initLogging(config)

	if config.Secrets.DisableAuth {
		logrus.Warnln("log service is being started without auth, SHOULD NOT BE DONE FOR PROD ENVIRONMENTS")
	}

	var store store.Store
	if config.S3.Bucket != "" {
		// create the s3 store.
		logrus.Infof("configuring log store to use s3 compatible backend with endpoint: %s and bucket name: %s and ACL: %s",
			config.S3.Endpoint, config.S3.Bucket, config.S3.Acl)
		store = s3.NewEnv(
			config.S3.Bucket,
			config.S3.Prefix,
			config.S3.Endpoint,
			config.S3.PathStyle,
			config.S3.AccessKeyID,
			config.S3.AccessKeySecret,
			config.S3.Region,
			config.S3.Acl,
		)
	} else {
		// create the blob store.
		store, err = bolt.New(config.Bolt.Path)
		if err != nil {
			logrus.WithError(err).
				Fatalln("cannot initialize the bolt database")
			return err
		}

		logrus.Warnln("the bolt datastore is configured")
		logrus.Warnln("the bolt datastore is suitable for testing purposes only")
	}

	// create the stream server.
	var stream stream.Stream
	if config.Redis.Endpoint != "" {
		stream = redis.New(config.Redis.Endpoint, config.Redis.Password)
		logrus.Infof("configuring log stream to use Redis: %s", config.Redis.Endpoint)
	} else {
		// create the in-memory stream
		stream = memory.New()
		logrus.Infoln("configuring log stream to use in-memory stream")
	}

	// create the http server.
	server := server.Server{
		Acme:    config.Server.Acme,
		Addr:    config.Server.Bind,
		Handler: handler.Handler(stream, store, config),
	}

	// trap the os signal to gracefully shutdown the
	// http server.
	ctx := context.Background()
	ctx, cancel := context.WithCancel(ctx)
	s := make(chan os.Signal, 1)
	signal.Notify(s, os.Interrupt)
	defer func() {
		signal.Stop(s)
		cancel()
	}()
	go func() {
		select {
		case val := <-s:
			logrus.Infof("received OS Signal to exit xxxxxxxx %s", val)
			cancel()
		case <-ctx.Done():
			logrus.Infoln("received a done signal to exit server")
		}
	}()

	logrus.Infof(fmt.Sprintf("server listening at port %s", config.Server.Bind))

	// starts the http server.
	err = server.ListenAndServe(ctx)
	if err == context.Canceled {
		logrus.Infoln("program gracefully terminated")
		return nil
	}

	if err != nil {
		logrus.Errorf("program terminated with error: %s", err)
	}

	return err
}

// Register the server commands.
func Register(app *kingpin.Application) {
	c := new(serverCommand)

	cmd := app.Command("server", "start the server").
		Action(c.run)

	cmd.Flag("env-file", "environment file").
		Default(".env").
		StringVar(&c.envfile)
}

// Get stackdriver to display logs correctly
// https://github.com/sirupsen/logrus/issues/403
// TODO: (Vistaar) Move to uber zap similar to other services
type OutputSplitter struct{}

func (splitter *OutputSplitter) Write(p []byte) (n int, err error) {
	if bytes.Contains(p, []byte("level=error")) {
		return os.Stderr.Write(p)
	}
	return os.Stdout.Write(p)
}

// helper function configures the global logger from
// the loaded configuration.
func initLogging(c config.Config) {
	logrus.SetOutput(&OutputSplitter{})
	l := logrus.StandardLogger()
	logger.L = logrus.NewEntry(l)
	if c.Debug {
		l.SetLevel(logrus.DebugLevel)
	}
	if c.Trace {
		l.SetLevel(logrus.TraceLevel)
	}
}
