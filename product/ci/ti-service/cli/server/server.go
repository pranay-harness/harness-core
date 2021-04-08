package server

import (
	"context"
	"errors"
	"fmt"
	"os"
	"os/signal"

	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/db/timescaledb"
	"github.com/wings-software/portal/product/ci/ti-service/eventsframework"
	"github.com/wings-software/portal/product/ci/ti-service/handler"
	"github.com/wings-software/portal/product/ci/ti-service/server"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"

	"github.com/joho/godotenv"
	kingpin "gopkg.in/alecthomas/kingpin.v2"
)

type serverCommand struct {
	envfile string
}

func (c *serverCommand) run(*kingpin.ParseContext) error {
	godotenv.Load(c.envfile)

	ctx := context.Background()

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

	if config.Secrets.DisableAuth {
		log.Warn("ti service is being started without auth, SHOULD NOT BE DONE FOR PROD ENVIRONMENTS")
	}

	// TODO: (vistaar) Rename to something better
	var db db.Db
	if config.TimeScaleDb.DbName != "" && config.TimeScaleDb.Host != "" {
		// Create timescaledb connection
		log.Infow("configuring TI service to use Timescale DB",
			"endpoint", config.TimeScaleDb.Host,
			"db_name", config.TimeScaleDb.DbName,
			"test_table_name", config.TimeScaleDb.HyperTableName,
			"selection_stats_table", config.TimeScaleDb.SelectionTable,
			"coverage_table", config.TimeScaleDb.CoverageTable)
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
		log.Errorw("timescale DB or host not configured properly")
		return errors.New("timescale db name not configured")
	}

	// Test intelligence DB
	var tidb tidb.TiDB
	if config.MongoDb.DbName != "" && (config.MongoDb.Host != "" || config.MongoDb.ConnStr != "") {
		// Create mongoDB connection
		log.Infow("configuring TI service to use mongo DB",
			"host", config.MongoDb.Host,
			"db_name", config.MongoDb.DbName)
		tidb, err = mongodb.New(
			config.MongoDb.Username,
			config.MongoDb.Password,
			config.MongoDb.Host,
			config.MongoDb.Port,
			config.MongoDb.DbName,
			config.MongoDb.ConnStr,
			log)
		if err != nil {
			log.Errorw("unable to connect to mongo DB")
			return errors.New("unable to connect to mongo DB")
		}

		// Test intelligence is configured. Start up redis watcher for watching push events
		if config.EventsFramework.RedisUrl != "" {
			log.Infow("connecting to redis for receiving events", "url", config.EventsFramework.RedisUrl)
			rdb, err := eventsframework.New(config.EventsFramework.RedisUrl,
				config.EventsFramework.RedisPassword,
				log)
			if err != nil {
				log.Errorw("could not establish connection with events framework Redis")
				return errors.New("could not establish connection with events framework Redis")
			}
			topic := fmt.Sprintf("%s:streams:webhook_request_payload_data", config.EventsFramework.EnvNamespace)
			log.Infow("registering webhook payload consumer with events framework", "topic", topic)
			rdb.RegisterMerge(ctx, topic, tidb.MergePartialCg, db, config)
			rdb.Run()
			log.Infow("done registering webhook consumer")
		} else {
			log.Errorw("events framework redis URL not configured")
			// TODO: (vistaar) Remove this once redis client is stable
			//return errors.New("events framework redis URL not configured")
		}
	} else {
		log.Errorw("mongo DB not configured properly")
		return errors.New("mongo db info not configured")
	}

	// create the http server.
	server := server.Server{
		Acme:    config.Server.Acme,
		Addr:    config.Server.Bind,
		Handler: handler.Handler(db, tidb, config, log),
	}

	// trap the os signal to gracefully shutdown the
	// http server.
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

	log.Info(fmt.Sprintf("server listening at %s", config.Server.Bind))

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
