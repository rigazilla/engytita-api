package main

import (
	"fmt"
	"os"

	"github.com/engytita/engytita-api/examples/golang/config/cache/v1alpha"
	"github.com/golang/protobuf/proto"
	"google.golang.org/protobuf/encoding/protojson"
	yamme "sigs.k8s.io/yaml"
)

//go:generate protoc --proto_path=../../..  --go_out=.. --go_opt=module=github.com/engytita/engytita-api/example/golang config/cache/v1alpha/region.proto config/cache/v1alpha/cache.proto config/cache/v1alpha/datasource.proto

func main() {
	yaml := `
name: cacheExample
namespace: nsExample
regions:
  - name: Region1
    datasource: Datasource1
    rule:
        jsonpath:
          value: some.domain.stores
    expiration:
        schedule: 0 0 1 * *
  - name: Region2
    datasource: Datasource2
    rule:
        wildcard:
          value: /pets/(.*)
    preload:
        http:
          url: value
        schedule: 0 0 1 * *
    expiration:
        lifespan: 86400s
    bound:
        count:
          value: 1000
`
	json, err := yamme.YAMLToJSON([]byte(yaml))
	if err != nil {
		fmt.Printf("json err: %v\n", err)
	}
	cache := &v1alpha.Cache{}
	err = protojson.Unmarshal(json, cache)
	if err != nil {
		fmt.Printf("proto err: %v\n", err)
	}
	printer := proto.TextMarshaler{}
	fmt.Println("============ Readable Protobuf Output =============")
	printer.Marshal(os.Stdout, cache)
}
