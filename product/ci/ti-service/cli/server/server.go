package server

import (
	"context"
	"errors"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"os"
	"os/signal"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/db/timescaledb"
	"github.com/wings-software/portal/product/ci/ti-service/handler"
	"github.com/wings-software/portal/product/ci/ti-service/server"

	"github.com/joho/godotenv"
	kingpin "gopkg.in/alecthomas/kingpin.v2"
)

type serverCommand struct {
	envfile string
}

func (c *serverCommand) run(*kingpin.ParseContext) error {
	godotenv.Load(c.envfile)

	// build initial log
	logBuilder := logs.NewBuilder().Verbose(true).WithDeployment("ti-service").
		WithFields("application_name", "TI-svc")
	log := logBuilder.MustBuild().Sugar()

	// load the system configuration from the environment.
	config, err := config.Load()
	if err != nil {
		log.Errorw("cannot load service configuration", zap.Error(err))
		return err
	}

	var db db.Db
	if config.TimeScaleDb.DbName != "" && config.TimeScaleDb.Host != "" {
		// Create timescaledb connection
		log.Infow("configuring TI service to use Timescale DB",
			"endpoint", config.TimeScaleDb.Host,
			"db_name", config.TimeScaleDb.DbName)
		db, err = timescaledb.New(
			config.TimeScaleDb.Username,
			config.TimeScaleDb.Password,
			config.TimeScaleDb.Host,
			config.TimeScaleDb.Port,
			config.TimeScaleDb.DbName,
			log,
		)
		if err != nil {
			log.Errorw("error while trying to connect to timescale DB ", zap.Error(err))
			return err
		}
	} else {
		// TODO (Vistaar): Use in-memory DB if timescaledb is not set up
		log.Errorw("timescale DB or host not configured properly")
		return errors.New("timescale db name not configured")
	}

	// create the http server.
	server := server.Server{
		Acme:    config.Server.Acme,
		Addr:    config.Server.Bind,
		Handler: handler.Handler(db, config, log),
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
			log.Info("received OS Signal to exit xxxxxxxx %s", val)
			cancel()
		case <-ctx.Done():
			log.Info("received a done signal to exit server")
		}
	}()

	log.Info("server listening at %s", config.Server.Bind)

	// starts the http server.
	err = server.ListenAndServe(ctx)
	if err == context.Canceled {
		log.Info("program gracefully terminated")
		return nil
	}

	if err != nil {
		log.Errorw("program terminated", zap.Error(err))
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
