/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.http;import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.GHResponse;
import com.graphhopper.Trip;
import com.graphhopper.http.GHPointConverterProvider;
import com.graphhopper.jackson.Jackson;
import com.graphhopper.reader.gtfs.GraphHopperGtfs;
import com.graphhopper.reader.gtfs.GtfsStorage;
import com.graphhopper.reader.gtfs.PtFlagEncoder;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.Helper;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.File;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ResourceIT {

    private static final String GRAPH_LOC = "target/ResourceIT";
    private static GraphHopperGtfs graphHopper;
    private static final ZoneId zoneId = ZoneId.of("America/Los_Angeles");
    private static GraphHopperStorage graphHopperStorage;
    private static LocationIndex locationIndex;
    private static GtfsStorage gtfsStorage;

    static {
        Helper.removeDir(new File(GRAPH_LOC));
        final PtFlagEncoder ptFlagEncoder = new PtFlagEncoder();
        final CarFlagEncoder carFlagEncoder = new CarFlagEncoder();
        final FootFlagEncoder footFlagEncoder = new FootFlagEncoder();

        EncodingManager encodingManager = EncodingManager.create(Arrays.asList(carFlagEncoder, ptFlagEncoder, footFlagEncoder), 8);
        GHDirectory directory = GraphHopperGtfs.createGHDirectory(GRAPH_LOC);
        gtfsStorage = GraphHopperGtfs.createGtfsStorage();
        graphHopperStorage = GraphHopperGtfs.createOrLoad(directory, encodingManager, ptFlagEncoder, gtfsStorage, Collections.singleton("files/sample-feed.zip"), Collections.emptyList());
        locationIndex = GraphHopperGtfs.createOrLoadIndex(directory, graphHopperStorage);
        graphHopper = GraphHopperGtfs.createFactory(ptFlagEncoder, GraphHopperGtfs.createTranslationMap(), graphHopperStorage, locationIndex, gtfsStorage)
                .createWithoutRealtimeFeed();
    }


    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addProvider(new GHPointConverterProvider())
            .setMapper(Jackson.newObjectMapper())
            .addResource(graphHopper)
            .build();

    @Test
    public void testStationStationQuery() throws Exception {
        final Response response = resources.target("/route?point=Stop(070201053201)&point=Stop(070201053801)&locale=en-US&vehicle=pt&weighting=fastest&elevation=false&pt.earliest_departure_time=2017-08-28T08%3A46%3A46.649Z&use_miles=false&points_encoded=false&pt.max_walk_distance_per_leg=1000&pt.max_transfer_distance_per_leg=0&pt.profile=true&pt.limit_solutions=5").request().buildGet().invoke();
        assertEquals(200, response.getStatus());
        GHResponse ghResponse = response.readEntity(GHResponse.class);
        System.out.println(ghResponse);
    }

    @Test
    public void testPointPointQuery() throws Exception {
        final Response response = resources.target("/route?point=52.5141,13.4963&point=52.5137,13.4355&locale=en-US&vehicle=pt&weighting=fastest&elevation=false&pt.earliest_departure_time=2017-08-28T08%3A46%3A46.649Z&use_miles=false&points_encoded=false&pt.max_walk_distance_per_leg=1000&pt.max_transfer_distance_per_leg=0&pt.profile=true&pt.limit_solutions=5").request().buildGet().invoke();
        assertEquals(200, response.getStatus());
        JsonNode json = response.readEntity(JsonNode.class);
        JsonNode path2 = json.get("paths").get(2);
        JsonNode path3 = json.get("paths").get(3);
        path2.get("legs").get(path2.get("legs").size()-1).get("edges").forEach(n -> System.out.println(n.get("adjNode").asLong()));
        System.out.println("---");
        path3.get("legs").get(path3.get("legs").size()-1).get("edges").forEach(n -> System.out.println(n.get("adjNode").asLong()));

    }

    @Test
    public void testRouteNotFoundCase() {
        final Response response = resources.target("/route?vehicle=pt&point=52.553423,13.435518&point=52.591982,13.305924&pt.earliest_departure_time=2017-09-01T09%3A47%3A00.000Z&pt.profile=true").request().buildGet().invoke();
        System.out.println(response.readEntity(String.class));
        assertEquals(200, response.getStatus());
    }

}
