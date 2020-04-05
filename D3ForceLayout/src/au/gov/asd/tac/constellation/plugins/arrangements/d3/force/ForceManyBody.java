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
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.D3AfterVisitor;
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.D3Quad;
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.D3QuadNode;
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.D3QuadTree;
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.D3Visitor;
import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.IPoint;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author algol
 */
public class ForceManyBody implements Force {
    private List<IVertex> vxs;
    private double strength;
    private double[] strengths;
    private double distanceMin2;
    private double distanceMax2;
    private double theta2;

    /**
     * Per D3Node metadata for many-body algorithm.
     */
    public static class ForceMeta {
        double x = 0;
        double y = 0;
        double value = 0;

        @Override
        public String toString() {
            return String.format("Meta[x=%s,y=%s,s=%s]", x, y, value);
        }
    }

    private final DefaultHashMap<Integer, ForceMeta> forceMetas;

    public ForceManyBody() {
        forceMetas = new DefaultHashMap<>(ForceMeta.class);
    }

    @Override
    public void initialise(final List<IVertex> vxs) {
        this.vxs = vxs;
        final int n = vxs.size();

        strength = -30;
        strengths = new double[n];
        for(int i=0; i<n; i++) {
            final IVertex v = vxs.get(i);
            strengths[v.getIndex()] = strength;
        }

        theta2 = 0.81;
        distanceMin2 = 1;
        distanceMax2 = Double.POSITIVE_INFINITY;
    }

    @Override
    public void force(final double alpha) {
        final D3QuadTree tree = new D3QuadTree();
//        System.out.printf("@@add\n");
//        vxs.forEach(vx -> {System.out.printf("@vx %s\n", vx);});
        final List<IPoint> vv = new ArrayList<>(vxs);
        tree.add(vv);
//        System.out.printf("@@accum\n");
        tree.visitAfter(new Accumulate());
//        tree.visitAfter((final D3Quad quad) -> {
//            System.out.printf("@@> quad %s\n", quad);
//        });
//        System.out.printf("@@apply\n");
        final Apply apply = new Apply();
        vxs.forEach(vx -> {
            apply.setCurrent(vx, alpha);
            tree.visit(apply);
        });
    }

    private class Accumulate implements D3AfterVisitor {
        @Override
        public void callback(final D3Quad quad) {
            double strength = 0;
            double weight = 0;
            if(!quad.node.isLeaf()) {
                // For internal nodes, accumulate forces from child quadrants.
                //
                double x = 0;
                double y = 0;
                for(int i=0; i<D3QuadNode.NQUADS; i++) {
                    final D3QuadNode q = quad.node.getNode(i);
                    if(q!=null) {
                        final ForceMeta meta = forceMetas.get(q.id);
                        final double c = Math.abs(meta.value);
                        if(c!=0) {
                            strength += meta.value;
                            weight += c;
                            x += c * meta.x;
                            y += c * meta.y;
                        }
                    }
                }

                final ForceMeta forceMeta = forceMetas.get(quad.node.id);
                forceMeta.x = x / weight;
                forceMeta.y = y / weight;
//                System.out.printf("@@accum1 %s %s %s %s\n", strength, forceMeta.x, forceMeta.y, weight);
            } else {
                // For leaf nodes, accumulate forces from coincident quadrants.
                //
                final D3QuadNode q = quad.node;
                final ForceMeta forceMeta = forceMetas.get(q.id);
                final IVertex vx = (IVertex)q.getData().get(0);
                forceMeta.x = vx.getX();
                forceMeta.y = vx.getY();
                for(var point : q.getData()) {
                    strength += strengths[((IVertex)point).getIndex()];
                }
//                System.out.printf("@@accum2 %s %s %s %s\n", strength, forceMeta.x, forceMeta.y, weight);
            }

            forceMetas.get(quad.node.id).value = strength;
        }
    }

    private class Apply implements D3Visitor {
        private IVertex currentVx;
        private double alpha;

        void setCurrent(final IVertex vx, final double alpha) {
            currentVx = vx;
            this.alpha = alpha;
        }

        @Override
        public boolean callback(final D3Quad quad) {
            final ForceMeta meta = forceMetas.get(quad.node.id);
            if(meta.value==0) {
//                System.out.printf("@@quad.value %s\n", meta.value);
                return true;
            }

            double x = meta.x - currentVx.getX();
            double y = meta.y - currentVx.getY();
            final double w = quad.x1 - quad.x0;
            double l = x*x + y*y;
//            System.out.printf("@@0 x y w %s %s %s\n", x, y, w);

            // Apply the Barnes-Hut approximation if possible.
            // Limit forces for very close nodes; randomize direction if coincident.
            //
//            System.out.printf("@@1 w2=%s theta2=%s /=%s l=%s\n", w*w, theta2, w*w/theta2, l);
            if(w*w/theta2<l) {
//                System.out.printf("@@2 l=%s distanceMax2=%s\n", l, distanceMax2);
                if(l<distanceMax2) {
                    if(x==0) {
                        x = jiggle();
                        l += x*x;
                    }
                    if(y==0) {
                        y = jiggle();
                        l += y*y;
                    }
                    if(l<distanceMin2) {
                        l = Math.sqrt(distanceMin2*l);
                    }

                    final double xvel = x*meta.value*alpha/l;
                    currentVx.setXVelocity(currentVx.getXVelocity()+xvel);
                    final double yvel = y*meta.value*alpha/l;
                    currentVx.setYVelocity(currentVx.getYVelocity()+yvel);
//                    System.out.printf("@@a x=%s y=%s %s %s %s\n", x, y, meta.value, alpha, l);
//                    System.out.printf("@@vela %s %s\n", currentVx.getXVelocity(), currentVx.getYVelocity());
                }

                return true;
            } else if(!quad.node.isLeaf() || l>=distanceMax2) {
//                System.out.printf("@@early %s %s %s\n", l, distanceMax2, quad.node);
                return false;
            }
            // Otherwise, process points directly.
            //

            // Limit forces for very close nodes; randomize direction if coincident.
            //
            final IVertex quadVx = (IVertex)quad.node.getData().get(0);
            if(quadVx.getIndex()!=currentVx.getIndex() || quad.node.getData().size()>1) {
                if(x==0) {
                    x = jiggle();
                    l += x*x;
                }
                if(y==0) {
                    y = jiggle();
                    l += y*y;
                }
                if(l<distanceMin2) {
                    l = Math.sqrt(distanceMin2*l);
                }
//                System.out.printf("@@close x y l %s %s %s\n", x, y ,l);
            }

            for(final IPoint p : quad.node.getData()) {
                final IVertex vx = (IVertex)p;
//                System.out.printf("@@pre %s %s %s\n", vx.getIndex()!=currentVx.getIndex(), vx.getIndex(), currentVx.getIndex());
                if(vx.getIndex()!=currentVx.getIndex()) {
//                    System.out.printf("@@index %s %s %s %s\n", vx.getIndex(), currentVx.getIndex(), vx.getXVelocity(), vx.getYVelocity());
                    final double s = strengths[vx.getIndex()] * alpha/l;
                    currentVx.setXVelocity(currentVx.getXVelocity() + x*s);
                    currentVx.setYVelocity(currentVx.getYVelocity() + y*s);
//                    System.out.printf("@@b x=%s y=%s %s\n", x, y ,s);
//                    System.out.printf("@@velb %s %s %s %s\n", vx.getIndex(), s, currentVx.getXVelocity(), currentVx.getYVelocity());
                }
            }

            return false;
        }
    }
}
