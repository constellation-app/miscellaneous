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

import static au.gov.asd.tac.constellation.plugins.arrangements.d3.force.V.v;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

/**
 *
 * @author algol
 */
public class ForceManyBodyNGTest {
    @Test(description="basic many body")
    public void forceManyBody() {
        final List<IVertex> vxs = new ArrayList();
        for(int i=0; i<6; i++) {
            vxs.add(v(Double.NaN , Double.NaN));
//            vxs.add(v(i, i));
        }
        System.out.printf("%s\n", vxs);
        final Simulation sim = new Simulation(vxs);
        sim.addForce("many_body", new ForceManyBody());
        sim.addForce("centre", new ForceCentre(960/2, 600/2));
        sim.step();

        System.out.printf("%s\n", vxs);
//        assertEquals(toString(vxs), "[[-0.5,-0.5,1.0],[-0.5,0.5,1.0],[0.5,0.5,1.0],[0.5,-0.5,1.0]]");
    }

//    @Test(description="basic many body 2")
//    public void forceManyBody2() {
//        final Random r = new Random();
//        final List<IVertex> vxs = new ArrayList();
//        for(int i=0; i<100; i++) {
////            vxs.add(v(r.nextFloat(), r.nextFloat()));
//            vxs.add(v(Float.NaN , Float.NaN));
//        }
////        System.out.printf("%s\n", vxs);
//        final Simulation sim = new Simulation(vxs);
////        sim.addForce("many_body", new ForceManyBody());
////        sim.step();
//
//        System.out.printf("%s\n", vxs);
//    }
}
