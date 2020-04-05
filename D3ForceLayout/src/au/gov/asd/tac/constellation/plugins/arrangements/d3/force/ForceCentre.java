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

import java.util.List;

/**
 * Position the vertices around the given centre.
 *
 * @author algol
 */
public class ForceCentre implements Force {
    private List<IVertex> vxs;
    private final double centreX;
    private final double centreY;

    public ForceCentre(final double centreX, final double centreY) {
        this.centreX = centreX;
        this.centreY = centreY;
    }

    @Override
    public void initialise(final List<IVertex> vxs) {
        this.vxs = vxs;
    }

    @Override
    public void force(final double alpha) {
        double sx = 0;
        double sy = 0;

        for(final IVertex vx : vxs) {
            sx += vx.getX();
            sy += vx.getY();
        }

        final double fx = sx/vxs.size() - centreX;
        final double fy = sy/vxs.size() - centreY;
        vxs.forEach(vx -> {
            vx.setX(vx.getX()-fx);
            vx.setY(vx.getY()-fy);
        });
    }
}
