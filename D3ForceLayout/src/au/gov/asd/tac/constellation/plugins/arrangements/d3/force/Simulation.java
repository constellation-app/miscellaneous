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

import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author algol
 */
public final class Simulation {
    private final List<IVertex> vxs;
    private double alpha;
    private double alphaMin;
    private double alphaDecay;
    private double alphaTarget;
    private double velocityDecay;
    private LinkedHashMap<String, Force> forces;

    final double initialRadius = 10;
    final double initalAngle = Math.PI * (3-Math.sqrt(5));

    public Simulation(final List<IVertex> vxs) {
        this.vxs = vxs;
        alpha = 1;
        alphaMin = 0.001;
        alphaDecay = 1.0 - Math.pow(alphaMin, 1.0/300.0);
        alphaTarget = 0;
        velocityDecay = 0.6;
        forces = new LinkedHashMap<>();

        initialiseVertices();
    }

    public Simulation addForce(final String name, final Force force) {
        forces.put(name, force);
        force.initialise(vxs);

        return this;
    }

    private void initialiseVertices() {
        int i = 0;
        for(final IVertex vx : vxs) {
            vx.setIndex(i);
            if(Double.isNaN(vx.getX()) || Double.isNaN(vx.getY())) {
                final double radius = initialRadius * Math.sqrt(i);
                final double angle = i * initalAngle;
                vx.setX((radius * Math.cos(angle)));
                vx.setY((radius * Math.sin(angle)));
            }

            if(true) { // Double.isNaN(vx.getXVelocity()) || Double.isNaN(vx.getYVelocity())) {
                vx.setXVelocity(0);
                vx.setYVelocity(0);
            }

            i++;
        }
    }

    void step() {
        while(alpha>=alphaMin) {
            tick(1);
//            alpha = 0; // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        }
    }

    void tick(final int iterations) {
        for(int k=0; k<iterations; k++) {
            alpha += (alphaTarget - alpha) * alphaDecay;

            forces.forEach((name, force) -> {
//                System.out.printf("@@force %s %s %s\n", name, force, alpha);
                force.force(alpha);
            });

            int[] _i = new int[1];
            vxs.forEach(vx -> {
//                System.out.printf("@@vel %s %s\n", vx.getXVelocity(), vx.getYVelocity());
                vx.setXVelocity(vx.getXVelocity() * velocityDecay);
                vx.setX(vx.getX() + vx.getXVelocity());

                vx.setYVelocity(vx.getYVelocity() * velocityDecay);
                vx.setY(vx.getY() + vx.getYVelocity());

                System.out.printf("@@tick %s %s %s\n", _i[0]++, vx.getX(), vx.getY());
            });
            System.out.printf("\n");
        }
    }
}
