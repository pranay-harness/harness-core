package avro

import (
	"fmt"
	"io/ioutil"

	"github.com/linkedin/goavro/v2"
)

// CallgrSerialzer struct implementing NewCallgrSer interface
type CallgrSerialzer struct {
	codec *goavro.Codec
}

// NewCallgrSer returns new NewCallgrSer object with the codec
// based on the schema received in the input
func NewCallgrSer(file string) (*CallgrSerialzer, error) {
	schema, err := ioutil.ReadFile(file)
	fmt.Print(string(schema))
	if err != nil {
		// handle this seperately
		panic(err)
	}

	codec, err := goavro.NewCodec(string(schema))
	if err != nil {
		panic(err)
	}

	return &CallgrSerialzer{
		codec: codec,
	}, nil
}

//Serialzer is the interface for encoding and decoding structs
type Serialzer interface {
	// Serialize given struct and return the binary value
	Serialize(datum interface{}) ([]byte, error)
	// Deserialize given struct and return the decoded interface{}
	Deserialize(buf []byte) (interface{}, error)
}

//Serialize a given struct interface and return the byte array and error if any
func (c *CallgrSerialzer) Serialize(datum interface{}) ([]byte, error) {
	bin, err := c.codec.BinaryFromNative(nil, datum)
	if err != nil {
		fmt.Println("error")
		panic(err)
	}
	return bin, nil
}

//Deserialize a interface and return a Byte array which can be decoded in corresponding struct
func (c *CallgrSerialzer) Deserialize(buf []byte) (interface{}, error) {
	native, _, err := c.codec.NativeFromBinary(buf)
	if err != nil {
		return nil, err
	}
	return native, nil
}

//Relation b/w source and test
type Relation struct {
	Source int   `json:"source"`
	Tests  []int `json:"tests"`
}
