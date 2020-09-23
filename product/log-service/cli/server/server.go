package server

import (
	"context"
	"os"
	"os/signal"

	"github.com/wings-software/portal/product/log-service/handler"
	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/server"
	"github.com/wings-software/portal/product/log-service/store"
	"github.com/wings-software/portal/product/log-service/store/bolt"
	"github.com/wings-software/portal/product/log-service/store/minio"
	"github.com/wings-software/portal/product/log-service/stream/memory"

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
	config, err := Load()
	if err != nil {
		logrus.WithError(err).
			Errorln("cannot load the service configuration")
		return err
	}

	// init the system logging.
	initLogging(config)

	var store store.Store
	if config.Minio.Bucket != "" {
		// create the minio store.
		store = minio.NewEnv(
			config.Minio.Bucket,
			config.Minio.Prefix,
			config.Minio.Endpoint,
			config.Minio.PathStyle,
		)
		if err != nil {
			logrus.WithError(err).
				Fatalln("cannot initialize the minio database")
			return err
		}
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
	stream := memory.New()

	// create the http server.
	server := server.Server{
		Acme:    config.Server.Acme,
		Addr:    config.Server.Bind,
		Handler: handler.Handler(stream, store),
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
		case <-s:
			cancel()
		case <-ctx.Done():
		}
	}()

	logrus.Infof("server listening at %s", config.Server.Bind)

	// starts the http server.
	err = server.ListenAndServe(ctx)
	if err == context.Canceled {
		logrus.Infoln("program gracefully terminated")
		return nil
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

// helper function configures the global logger from
// the loaded configuration.
func initLogging(c Config) {
	l := logrus.StandardLogger()
	logger.L = logrus.NewEntry(l)
	if c.Debug {
		l.SetLevel(logrus.DebugLevel)
	}
	if c.Trace {
		l.SetLevel(logrus.TraceLevel)
	}
}
