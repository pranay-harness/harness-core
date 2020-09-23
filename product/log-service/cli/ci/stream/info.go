package cistream

import (
	"encoding/json"
	"os"

	ciclient "github.com/wings-software/portal/product/log-service/client/ci"

	"gopkg.in/alecthomas/kingpin.v2"
)

type infoCommand struct {
	server string
	token  string
}

func (c *infoCommand) run(*kingpin.ParseContext) error {
	client := ciclient.NewHTTPClient(c.server, c.token, false)
	info, err := client.Info(nocontext)
	if err != nil {
		return err
	}
	enc := json.NewEncoder(os.Stdout)
	enc.SetIndent("", "  ")
	return enc.Encode(info)
}

func registerInfo(app *kingpin.CmdClause) {
	c := new(infoCommand)

	cmd := app.Command("info", "return stream data").
		Action(c.run)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8080").
		StringVar(&c.server)

	cmd.Flag("token", "server token").
		StringVar(&c.token)
}
