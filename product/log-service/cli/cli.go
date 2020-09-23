package cli

import (
	"context"
	"os"

	cistore "github.com/wings-software/portal/product/log-service/cli/ci/store"
	cistream "github.com/wings-software/portal/product/log-service/cli/ci/stream"
	"github.com/wings-software/portal/product/log-service/cli/server"

	"gopkg.in/alecthomas/kingpin.v2"
)

// program version
var version = "0.0.0"

// empty context
var nocontext = context.Background()

// Command parses the command line arguments and then executes a
// subcommand program.
func Command() {
	app := kingpin.New("harness-logger", "harness logging service")

	server.Register(app)
	cistore.Register(app)
	cistream.Register(app)

	kingpin.Version(version)
	kingpin.MustParse(app.Parse(os.Args[1:]))
}
