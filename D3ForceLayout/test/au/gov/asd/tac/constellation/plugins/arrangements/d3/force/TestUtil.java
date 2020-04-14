/*
 * Copyright 2010-2020 Australian Signals Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.gov.asd.tac.constellation.plugins.arrangements.d3.force;

import static au.gov.asd.tac.constellation.plugins.arrangements.d3.force.L.l;
import static au.gov.asd.tac.constellation.plugins.arrangements.d3.force.V.v;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author algol
 */
public class TestUtil {
    /**
     * A graph for testing stuff.
     * <p>
     * Totally nothing to do with CONSTELLATION's Graph.
     */
    public static class TGraph {
        final List<IVertex> vxs;
        final List<ILink> links;

        public TGraph(final List<IVertex> vxs, final List<ILink> links) {
            this.vxs = vxs;
            this.links = links;
        }
    }

    public static TGraph readGraph(final InputStream in) {
        final Map<String, IVertex> vxMap = new HashMap<>();
        final List<IVertex> vxs = new ArrayList<>();
        final List<ILink> links = new ArrayList<>();

        try(final BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            r.lines().forEachOrdered(line -> {
//                System.out.printf("%s\n", line);
                if(line.indexOf(',')==-1) {
                    final V vx = v(Double.NaN, Double.NaN);
                    vx.setLabel(line);
                    vxMap.put(line, vx);
                    vxs.add(vx);
                } else {
                    final String[] link = line.split(",");
                    final IVertex source = vxMap.get(link[0]);
                    final IVertex target = vxMap.get(link[1]);
                    links.add(l(source, target));
                }
            });
        } catch(final IOException ex) {
            Logger.getLogger(TestUtil.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new TGraph(vxs, links);
    }

    public static TGraph buildTreeGraph(final int n, final int maxChildren) {
        final List<IVertex> vxs = new ArrayList<>();
        final List<ILink> links = new ArrayList<>();

        final V root = v(Double.NaN, Double.NaN);
        root.setLabel("root");
        vxs.add(root);

        final Random r = new Random();
        for(int i=0; i<n; i++) {
            final IVertex parent = vxs.get(i);
//            final int nChildren = r.nextInt(maxChildren) + 1;
            final int nChildren = i<n/2 ? 3 : 10;
            for(int j=0; j<nChildren; j++) {
                final V vx = v(Double.NaN, Double.NaN);
                vx.setLabel(String.format("v%d-%d", i, j));
                vxs.add(vx);

                links.add(l(parent, vx));
            }
        }

        return new TGraph(vxs, links);
    }

    static void writePointsXY(final TGraph graph, final String fnam, final boolean includeLinks) throws FileNotFoundException {
        try(final PrintWriter out = new PrintWriter(fnam)) {
            graph.vxs.forEach(vx -> {
                final V v = (V)vx;
                out.printf("%s,%s,%s\n", v.getLabel().replace(".", ""), v.getX(), v.getY());
            });

            if(includeLinks) {
                graph.links.forEach(link -> {
                    out.printf("-,%s,%s\n", ((V)link.getSource()).getLabel(), ((V)link.getTarget()).getLabel());
                });
            }
        }
    }
}
