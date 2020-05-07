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

import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 *
 * @author algol
 */
public class ForceCentreNGTest {
    private static V v(final double x, double y) {
        return new V(x, y);
    }

    private static String toString(final List<IVertex> vxs) {
        final List<String> buf = new ArrayList<>();
        vxs.forEach(vx -> {
            buf.add(vx.toString());
        });

        return "[" + String.join(",", buf) + "]";
    }

    @Test(description="basic centre")
    public void forceCentre() {
        final List<IVertex> vxs = List.of(v(2, 2), v(2, 3), v(3, 3), v(3, 2));
        final Force centre = new ForceCentre(0, 0);
        centre.initialise(vxs);
        centre.force(Double.NaN);

        assertEquals(toString(vxs), "[[-0.5,-0.5,1.0],[-0.5,0.5,1.0],[0.5,0.5,1.0],[0.5,-0.5,1.0]]");
    }
}
