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

import au.gov.asd.tac.constellation.plugins.arrangements.d3.force.TestUtil.Graph;
import org.testng.annotations.Test;

/**
 *
 * @author algol
 */
public class ForceLinkNGTest {
    @Test(description="Read graph")
    public void readGraph() {
        final Graph graph = TestUtil.readGraph(getClass().getResourceAsStream("miserables.txt"));
        final ForceLink forceLink = new ForceLink(graph.links);
        forceLink.initialise(graph.vxs);
    }

    @Test(description="Layout")
    public void layout() {
        final Graph graph = TestUtil.readGraph(getClass().getResourceAsStream("miserables.txt"));

        final Simulation sim = new Simulation(graph.vxs);
        sim.addForce("link", new ForceLink(graph.links));
        sim.addForce("charge", new ForceManyBody());
        sim.addForce("centre", new ForceCentre(0, 0));

//        for(final IVertex vx : graph.vxs) {
//            final V v = (V)vx;
//            System.out.printf("%s,%s,%s\n", v.getLabel().replace(".", ""), v.getX(), v.getY());
//        }

        sim.step();

        for(final IVertex vx : graph.vxs) {
            final V v = (V)vx;
            System.out.printf("%s,%s,%s\n", v.getLabel().replace(".", ""), v.getX(), v.getY());
        }
    }
}
