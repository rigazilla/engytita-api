package org.engytita.api;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.Duration;
import config.cache.v1alpha.*;

/**
 * Hello world!
 *
 */
public class AppProtobufToJson {
        // Conversion protobuf -> json
        public static void main(String[] args) {
                // Build region1
                // {"name": "region1", "datasource": "DataSource1"
                // , "rule": {"jsonpath": {"value": "some.domain.stores"}}
                // , "expiration": {"schedule": "0 0 1 * *"}}
                Jsonpath.Builder jp = Jsonpath.newBuilder().setValue("some.domain.stores");
                List<Region> regions = new ArrayList<Region>();
                Rule.Builder r1 = Rule.newBuilder().setJsonpath(jp);
                Expiration.Builder ex = Expiration.newBuilder().setSchedule("0 0 1 * *");
                Region.Builder r = Region.newBuilder().setName("region1")
                                .setDatasource("DataSource1").setExpiration(ex);
                r.setRule(r1);
                regions.add(r.build());
                // Build region2
                // {"name": "region2", "datasource": "DataSource2"
                // , "rule": {"wildcard": {"value": "/pets/(.*)"}}
                // , "preload": {"http": {"url": "value"},"schedule": "0 0 1 * *"}
                // ,"expiration": {"lifespan": "86400s"}
                // ,"bound": {"count": {"value": "1000"}}}
                Wildcard.Builder wc = Wildcard.newBuilder().setValue("/pets/(.*)");
                r = Region.newBuilder().setName("region2")
                                .setDatasource("DataSource2");
                Http.Builder ht = Http.newBuilder().setUrl("value");
                Count.Builder co = Count.newBuilder().setValue(1000);
                Bound.Builder bl = Bound.newBuilder().setCount(co);
                Preload.Builder pr = Preload.newBuilder().setSchedule("0 0 1 * *").setHttp(ht);
                r1 = Rule.newBuilder().setWildcard(wc);
                Duration.Builder ls = Duration.newBuilder().setSeconds(24 * 3600);
                ex = Expiration.newBuilder().setLifespan(ls);
                r.setRule(r1).setPreload(pr).setBound(bl).setExpiration(ex);
                regions.add(r.build());
                // Build cache
                Cache.Builder cb = Cache.newBuilder().setName("cacheExample")
                                .setNamespace("nsExample").addAllRegions(regions);
                Cache c = cb.build();
                try {
                        System.out.println(JsonFormat.printer().print(c));
                } catch (Exception e) {
                }
        }
}
