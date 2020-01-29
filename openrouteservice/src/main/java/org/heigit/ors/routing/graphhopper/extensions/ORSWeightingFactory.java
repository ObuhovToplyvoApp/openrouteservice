/*  This file is part of Openrouteservice.
 *
 *  Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.

 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License along with this library;
 *  if not, see <https://www.gnu.org/licenses/>.
 */
package org.heigit.ors.routing.graphhopper.extensions;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.*;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.TurnCostExtension;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import org.heigit.ors.routing.ProfileWeighting;
import org.heigit.ors.routing.graphhopper.extensions.flagencoders.FlagEncoderNames;
import org.heigit.ors.routing.graphhopper.extensions.weighting.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ORSWeightingFactory implements WeightingFactory {

    public ORSWeightingFactory() { }

    public Weighting enrichDefaultGHWeighting(Weighting result, HintsMap hintsMap, FlagEncoder encoder, GraphHopperStorage graphStorage) {

        // MARQ24 - this code need to be present ONLY once that's why I have deleted the same section that was
        // present in the graphhopper repro [com.graphhopper.GraphHopper]...
        TraversalMode tMode = encoder.supports(TurnWeighting.class) ? TraversalMode.EDGE_BASED : TraversalMode.NODE_BASED;
        if (hintsMap.has(Parameters.Routing.EDGE_BASED))
            tMode = hintsMap.getBool(Parameters.Routing.EDGE_BASED, false) ? TraversalMode.EDGE_BASED : TraversalMode.NODE_BASED;
        if (tMode.isEdgeBased() && !encoder.supports(TurnWeighting.class)) {
            throw new IllegalArgumentException("You need a turn cost extension to make use of edge_based=true, e.g. use car|turn_costs=true");
        }

        // Apply soft weightings
        if (hintsMap.getBool("custom_weightings", false)) {
            Map<String, String> map = hintsMap.toMap();

            List<String> weightingNames = new ArrayList<>();
            for (Map.Entry<String, String> kv : map.entrySet()) {
                String name = ProfileWeighting.decodeName(kv.getKey());
                if (name != null && !weightingNames.contains(name))
                    weightingNames.add(name);
            }

            List<Weighting> softWeightings = new ArrayList<>();

            for (String weightingName : weightingNames) {
                switch (weightingName) {
                    case "steepness_difficulty":
                        softWeightings.add(new SteepnessDifficultyWeighting(encoder, getWeightingProps(weightingName, map), graphStorage));
                        break;
                    case "avoid_hills":
                        softWeightings.add(new AvoidHillsWeighting(encoder, getWeightingProps(weightingName, map), graphStorage));
                        break;
                    case "green":
                        softWeightings.add(new GreenWeighting(encoder, getWeightingProps(weightingName, map), graphStorage));
                        break;
                    case "quiet":
                        softWeightings.add(new QuietWeighting(encoder, getWeightingProps(weightingName, map), graphStorage));
                        break;
                    case "acceleration":
                        softWeightings.add(new AccelerationWeighting(encoder, getWeightingProps(weightingName, map), graphStorage));
                        break;
                    default:
                        break;
                }
            }

            if (!softWeightings.isEmpty()) {
                Weighting[] arrWeightings = new Weighting[softWeightings.size()];
                arrWeightings = softWeightings.toArray(arrWeightings);
                result = new AdditionWeighting(arrWeightings, result, encoder);
            }
        }
        return result;
    }

    private PMap getWeightingProps(String weightingName, Map<String, String> map) {
        PMap res = new PMap();

        String prefix = "weighting_#" + weightingName;
        int n = prefix.length();

        for (Map.Entry<String, String> kv : map.entrySet()) {
            String name = kv.getKey();
            int p = name.indexOf(prefix);
            if (p >= 0)
                res.put(name.substring(p + n + 1, name.length()), kv.getValue());
        }

        return res;
    }
}