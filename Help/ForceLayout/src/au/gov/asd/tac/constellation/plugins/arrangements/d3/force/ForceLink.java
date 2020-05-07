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

import static au.gov.asd.tac.constellation.plugins.arrangements.d3.force.Util.jiggle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author algol
 */
public class ForceLink implements Force {
    private final List<ILink> links;
    private List<IVertex> vxs;
    private Map<Integer, IVertex> nodeById;
    private int[] count;
    private double[] bias;
    private double[] strengths;
    private double[] distances;

    private double distance;
    private int iterations;

    // The default strength is a method.
    // (Javascript is much looser about these things than Java.)
    // We want to use defaultStrength() by default, but a constant if neccessary.
    // Worry about more flexibility when we need it.
    //
    private Double strength;

    public ForceLink(final List<ILink> links) {
        this.links = links;
        distance = 30;
        iterations = 1;
        strength = null;
    }

    @Override
    public void initialise(final List<IVertex> vxs) {
        this.vxs = vxs;

        final int n = vxs.size();
        final int m = links.size();

        // Vertex instances must already have had their index initialised.
        //
        nodeById = new HashMap();
        vxs.forEach(vx -> {nodeById.put(vx.getIndex(), vx);});

        count = new int[n];
        for(int i=0; i<m; i++) {
            final ILink link = links.get(i);
            link.setIndex(i);

            count[link.getSource().getIndex()]++;
            count[link.getTarget().getIndex()]++;
        }

        bias = new double[m];
        for(int i=0; i<m; i++) {
            final ILink link = links.get(i);
            bias[i] = (double)count[link.getSource().getIndex()] /
                    (count[link.getSource().getIndex()] + count[link.getTarget().getIndex()]);
        }

        strengths = new double[m];
        initialiseStrength();
        distances = new double[m];
        initialiseDistance();
    }

    @Override
    public void force(double alpha) {
        final int n = links.size();
        for(int k=0; k<iterations; k++) {
            for(int i=0; i<n; i++) {
                final ILink link = links.get(i);
                final IVertex source = link.getSource();
                final IVertex target = link.getTarget();
                double x = target.getX() + target.getXVelocity() - source.getX() - source.getXVelocity();
                x = x!=0 ? x : jiggle();
                double y = target.getY() + target.getYVelocity() - source.getY() - target.getYVelocity();
                y = y!=0 ? y : jiggle();
                double l = Math.sqrt(x*x + y*y);
                l = (double)(l - distances[i]) / l * alpha * strengths[i];
                x *= l;
                y *= l;
                double b = bias[i];
                target.setXVelocity(target.getXVelocity() - x*b);
                target.setYVelocity(target.getYVelocity() - y*b);
                b = 1.0 - b;
                source.setXVelocity((source.getXVelocity() + x*b));
                source.setYVelocity((source.getYVelocity() + y*b));
            }
        }
    }

    double defaultStrength(final ILink link) {
        return 1.0 / Math.min(count[link.getSource().getIndex()], count[link.getTarget().getIndex()]);
    }

    void initialiseStrength() {
        System.out.printf("@@strength %s\n", strength);
        for(int i=0; i<links.size(); i++) {
            strengths[i] = strength==null ? defaultStrength(links.get(i)) : strength;
        }
    }

    void initialiseDistance() {
        for(int i=0; i<links.size(); i++) {
            distances[i] = distance;
        }
    }

    public double getDistance() {
        return distance;
    }

    public ForceLink setDistance(final double distance) {
        this.distance = distance;

        return this;
    }

    public Double getStrength() {
        return strength;
    }

    public ForceLink setStrength(final double strength) {
        this.strength = strength;

        return this;
    }

    public int getIterations() {
        return iterations;
    }

    public ForceLink setIterations(final int iterations) {
        this.iterations = iterations;

        return this;
    }

}
