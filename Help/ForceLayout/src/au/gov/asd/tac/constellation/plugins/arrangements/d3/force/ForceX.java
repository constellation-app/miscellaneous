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
 * Push nodes along the x dimension.
 * <p>
 * The strength of the force is proportional to the one-dimensional distance
 * between the node’s position and the target position.
 * While this force can be used to position individual nodes, it is intended
 * primarily for global forces that apply to all (or most) nodes.
 *
 * @author algol
 */
public class ForceX implements Force {
    private List<IVertex> vxs;
    private double[] strengths;
    private double[] xz;

    private double strength;

    public ForceX() {
        strength = 0.1;
    }

    @Override
    public void initialise(final List<IVertex> vxs) {
        this.vxs = vxs;
        final int n = vxs.size();
        strengths = new double[n];
        xz = new double[n];

        for(int i=0; i<n; i++) {
            final IVertex vx = vxs.get(i);
            xz[i] = vx.getX();
            strengths[i] = Double.isNaN(xz[i]) ? 0 : strength;
        }
    }

    @Override
    public void force(final double alpha) {
        final int n = vxs.size();
        for(int i=0; i<n; i++) {
            final IVertex vx = vxs.get(i);
            final double vel = (xz[i] - vx.getX()) * strengths[i] * alpha;
            vx.setXVelocity(vx.getXVelocity() + vel);
        }
    }

    public double getStrength() {
        return strength;
    }

    public ForceX setStrength(final double strength) {
        this.strength = strength;

        return this;
    }
}
