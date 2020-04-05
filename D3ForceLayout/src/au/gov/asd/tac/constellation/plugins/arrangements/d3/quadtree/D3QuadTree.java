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
package au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author algol
 */
public class D3QuadTree {
    private D3QuadNode root;

    private double extent_x0;
    private double extent_y0;
    private double extent_x1;
    private double extent_y1;

    public D3QuadTree() {
        root = null;

        // Initialise to an invalid extent.
        //
        extent_x0 = extent_y0 = 1;
        extent_x1 = extent_y1 = -1;
    }

    private D3QuadTree(final D3QuadNode root, final double x0, final double y0, final double x1, final double y1) {
        this.root = root!=null ? new D3QuadNode(root) : null;
        this.extent_x0 = x0;
        this.extent_y0 = y0;
        this.extent_x1 = x1;
        this.extent_y1 = y1;
    }

    public double[] getExtent(){
        return extent_x1<extent_x0 ? null : new double[]{extent_x0, extent_y0, extent_x1, extent_y1};
    }

    public D3QuadTree setExtent(final double x0, final double y0, final double x1, final double y1) {
        return cover(x0, y0).cover(x1, y1);
    }

    public D3QuadTree cover(final double x, final double y) {
        if(Double.isNaN(x) || Double.isNaN(y)) {
            // Ignore invalid points.
            //
            return this;
        }

        double x0 = extent_x0;
        double y0 = extent_y0;
        double x1 = extent_x1;
        double y1 = extent_y1;

        if(x1<x0) {
            // The quadtree has no extents, so initialize them.
            // Integer extents are necessary so that if we later double the extent,
            // the existing quadrant boundaries don’t change due to floating point error!
            //
            x0 = Math.floor(x);
            y0 = Math.floor(y);
            x1 = x0 + 1;
            y1 = y0 + 1;
        } else {
            double z = x1 - x0;

            while(x0 > x || x >= x1 || y0 > y || y >= y1) {
                final int i = ((y < y0)?1:0) << 1 | (x < x0?1:0);
                z *= 2;
                switch (i) {
                    case 0:
                        x1 = x0 + z;
                        y1 = y0 + z;
                        break;
                    case 1:
                        x0 = x1 - z;
                        y1 = y0 + z;
                        break;
                    case 2:
                        x1 = x0 + z;
                        y0 = y1 - z;
                        break;
                    case 3:
                        x0 = x1 - z;
                        y0 = y1 - z;
                        break;
                }
            }
        }

        extent_x0 = x0;
        extent_y0 = y0;
        extent_x1 = x1;
        extent_y1 = y1;

        return this;
    }

    public D3QuadTree add(final IPoint point) {
        return add(cover(point.getX(), point.getY()), point);
    }

    private static D3QuadTree add(final D3QuadTree tree, final IPoint point) {
        D3QuadNode node = tree.root;
        D3QuadNode leaf = new D3QuadNode(point);

        // If the tree is empty, initialize the root as a leaf.
        //
        if(node==null) {
            tree.root = leaf;
            return tree;
        }

        final double x = point.getX();
        final double y = point.getY();

        double x0 = tree.extent_x0;
        double y0 = tree.extent_y0;
        double x1 = tree.extent_x1;
        double y1 = tree.extent_y1;

        // Find the existing leaf for the new point, or add it.
        //
        D3QuadNode parent = null;
        int i = -1;
        while(!node.isLeaf()) {
            final double xm = (x0+x1)/2.0;
            final boolean right = x >= xm;
            if(right) {
                x0 = xm;
            } else {
                x1 = xm;
            }

            final double ym = (y0+y1)/2.0;
            final boolean bottom = y >= ym;
            if(bottom) {
                y0 = ym;
            } else {
                y1 = ym;
            }

            parent = node;
            i = ((bottom?1:0) << 1) | (right?1:0);
            node = node.getNode(i);
            if(node==null) {
                parent.getNodes()[i] = leaf;

                return tree;
            }
        }

        // Is the new point is exactly coincident with the existing point?
        //
        final double xp = node.getData().get(0).getX();
        final double yp = node.getData().get(0).getY();
        if(x==xp && y==yp) {
            node.getData().add(point);
            if(parent!=null) {
                parent.getNodes()[i] = node;
            } else {
                tree.root = node;
            }

            return tree;
        }

        // Otherwise, split the leaf node until the old and new point are separated.
        //
        int j = -1;
        do {
            if(parent!=null) {
                parent = parent.getNodes()[i] = new D3QuadNode();
            } else {
                parent = tree.root = new D3QuadNode();
            }

            final double xm = (x0 + x1)/2.0;
            final boolean right = x >= xm;
            if(right) {
                x0 = xm;
            } else {
                x1 = xm;
            }

            final double ym = (y0 + y1)/2.0;
            final boolean bottom = y >= ym;
            if(bottom) {
                y0 = ym;
            } else {
                y1 = ym;
            }

            i = ((bottom?1:0) << 1) | (right?1:0);
            j = (((yp>=ym)?1:0)<<1) | ((xp>=xm)?1:0);
        } while(i==j);

        parent.getNodes()[j] = node;
        parent.getNodes()[i] = leaf;

        return tree;
    }

    public D3QuadTree add(final List<IPoint> vxs) {
        double x0 = Double.POSITIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;

        // Compute the points and their extent.
        //
        for(final IPoint vx : vxs) {
            final double x = vx.getX();
            final double y = vx.getY();
            if(Double.isFinite(x) && Double.isFinite(y)) {
                if(x<x0) x0 = x;
                if(x>x1) x1 = x;
                if(y<y0) y0 = y;
                if(y>y1) y1 = y;
            }
        }

        // If there were no (valid) points, stop.
        //
        if(x0>x1 || y0>y1) {
            return this;
        }

        // Expand the tree to cover the new points.
        //
        cover(x0, y0).cover(x1, y1);

        // Add the new points.
        //
        vxs.forEach(vx -> {
            final double x = vx.getX();
            final double y = vx.getY();
            if(Double.isFinite(x) && Double.isFinite(y)) {
                add(this, vx);
            }
        });

        return this;
    }

    public IPoint find(final double x, final double y) {
        return find(x, y, Double.POSITIVE_INFINITY);
    }

    public IPoint find(final double x, final double y, final double radius) {
        final List<D3Quad> quads = new ArrayList<>();
        D3QuadNode node = root;
        if(node!=null) {
            quads.add((new D3Quad(node, extent_x0, extent_y0, extent_x1, extent_y1)));
        }

        double x0, y0, x3, y3;
        double r;
        if(Double.isInfinite(radius)) {
            x0 = extent_x0;
            y0 = extent_y0;
            x3 = extent_x1;
            y3 = extent_y1;
            r = Double.POSITIVE_INFINITY;
        } else {
            x0 = x - radius;
            y0 = y - radius;
            x3 = x + radius;
            y3 = y + radius;
            r = radius * radius;
        }

        IPoint data = null;

        while(!quads.isEmpty()) {
            final D3Quad q = quads.remove(quads.size()-1);

            // Stop searching if this quadrant can’t contain a closer node.
            //
            node = q.node;
            final double x1 = q.x0;
            final double y1 = q.y0;
            final double x2 = q.x1;
            final double y2 = q.y1;
            if(node==null || x1>x3 || y1>y3 || x2<x0 || y2<y0) {
                continue;
            }

            // Bisect the current quadrant.//
            if(!node.isLeaf()) {
                final double xm = (x1 + x2)/2.0;
                final double ym = (y1 + y2)/2.0;

                quads.add(new D3Quad(node.getNode(3), xm, ym, x2, y2));
                quads.add(new D3Quad(node.getNode(2), x1, ym, xm, y2));
                quads.add(new D3Quad(node.getNode(1), xm, y1, x2, ym));
                quads.add(new D3Quad(node.getNode(0), x1, y1, xm, ym));

                // Visit the closest quadrant first.
                //
                final int i = ((y>=ym?1:0)<<1) | (x>=xm?1:0);
                if(i!=0) {
                    final D3Quad qt = quads.get(quads.size()-1);
                    quads.set(quads.size()-1, quads.get(quads.size()-1-i));
                    quads.set(quads.size()-1-i, qt);
                }
            } else {
                // Visit this point. (Visiting coincident points isn’t necessary!)
                //
                final List<IPoint> vxs = node.getData();
                final IPoint vx = vxs.get(0);
                final double dx = x - vx.getX();
                final double dy = y - vx.getY();
                final double d2 = dx * dx + dy*dy;
                if(d2<r) {
                    r = d2;
                    final double d = Math.sqrt(r);
                    x0 = x - d;
                    y0 = y - d;
                    x3 = x + d;
                    y3 = y + d;
                    data = vx;
                }
            }
        }

        return data;
    }

    public D3QuadTree visit(final D3Visitor visitor) {
        final List<D3Quad> quads = new ArrayList<>();
        D3QuadNode node = root;
        if(node!=null) {
            quads.add((new D3Quad(root, extent_x0, extent_y0, extent_x1, extent_y1)));
        }

        while(!quads.isEmpty()) {
            final D3Quad quad = quads.remove(quads.size()-1);
            node = quad.node;
            if(!visitor.callback(quad) && !node.isLeaf()) {
                final double x0 = quad.x0;
                final double y0 = quad.y0;
                final double x1 = quad.x1;
                final double y1 = quad.y1;
                final double xm = (x0 + x1) / 2.0;
                final double ym = (y0 + y1) / 2.0;
                if(node.getNode(3)!=null) quads.add(new D3Quad(node.getNode(3), xm, ym, x1, y1));
                if(node.getNode(2)!=null) quads.add(new D3Quad(node.getNode(2), x0, ym, xm, y1));
                if(node.getNode(1)!=null) quads.add(new D3Quad(node.getNode(1), xm, y0, x1, ym));
                if(node.getNode(0)!=null) quads.add(new D3Quad(node.getNode(0), x0, y0, xm, ym));
            }
        }

        return this;
    }

    public D3QuadTree visitAfter(final D3AfterVisitor visitor) {
        final List<D3Quad> quads = new ArrayList<>();
        final List<D3Quad> next = new ArrayList<>();
        D3QuadNode node = root;
        if(node!=null) {
            quads.add((new D3Quad(root, extent_x0, extent_y0, extent_x1, extent_y1)));
        }

        while(!quads.isEmpty()) {
            final D3Quad quad = quads.remove(quads.size()-1);
            node = quad.node;
            if(!node.isLeaf()) {
                final double x0 = quad.x0;
                final double y0 = quad.y0;
                final double x1 = quad.x1;
                final double y1 = quad.y1;
                final double xm = (x0 + x1) / 2.0;
                final double ym = (y0 + y1) / 2.0;
                if(node.getNode(0)!=null) quads.add(new D3Quad(node.getNode(0), x0, y0, xm, ym));
                if(node.getNode(1)!=null) quads.add(new D3Quad(node.getNode(1), xm, y0, x1, ym));
                if(node.getNode(2)!=null) quads.add(new D3Quad(node.getNode(2), x0, ym, xm, y1));
                if(node.getNode(3)!=null) quads.add(new D3Quad(node.getNode(3), xm, ym, x1, y1));
            }

            next.add(quad);
        }

//        System.out.printf("@@visiting %s\n", next.size());
        while(!next.isEmpty()) {
            final D3Quad quad = next.remove(next.size()-1);
            visitor.callback(quad);
        }

        return this;
    }

    public D3QuadTree copy() {
        final D3QuadTree copy = new D3QuadTree(root, extent_x0, extent_y0, extent_x1, extent_y1);
        final D3QuadNode node = root;

        if(node==null) {
            return copy;
        }

        if(node.isLeaf()) {
            copy.root = node.copyLeaf();

            return copy;
        }

        final List<Object[]> nodes = new ArrayList<>();
        copy.root.clearNodes();
        nodes.add(new Object[]{node, copy.root});
        while(!nodes.isEmpty()) {
            final Object[] pair = nodes.remove(nodes.size()-1);
            final D3QuadNode source = (D3QuadNode)pair[0];
            final D3QuadNode target = (D3QuadNode)pair[1];
            for(int ix = 0; ix<4; ix++) {
                final D3QuadNode child = source.getNode(ix);
                if(child!=null) {
                    if(!child.isLeaf()) {
                        nodes.add(new Object[]{child, target.getNodes()[ix] = new D3QuadNode()});
                    } else {
                        target.getNodes()[ix] = child.copyLeaf();
                    }
                }
            }
        }

        return copy;
    }

    public D3QuadTree remove(final IPoint vx) {
        D3QuadNode node = root;
        if(root==null) {
            // If the tree is empty, initialize the root as a leaf.
            //
            return this;
        }

        final double x = vx.getX();
        final double y = vx.getY();

        // Find the leaf node for the point.
        // While descending, also retain the deepest parent with a non-removed sibling.
        //
        D3QuadNode parent = null;
        D3QuadNode retainer = null;
        int i = -1;
        int j = -1;
        if(!node.isLeaf()) {
            double x0 = extent_x0;
            double y0 = extent_y0;
            double x1 = extent_x1;
            double y1 = extent_y1;
            while (true) {
                final double xm = (x0 + x1) / 2.0;
                final boolean right = x >= xm;
                if(right) {
                    x0 = xm;
                } else {
                    x1 = xm;
                }

                final double ym = (y0 + y1) / 2.0;
                final boolean bottom = y >= ym;
                if(bottom) {
                    y0 = ym;
                } else {
                    y1 = ym;
                }

                parent = node;
                i = ((bottom?1:0) << 1) | (right?1:0);
                node = node.getNode(i);
                if(node==null) {
                    return this;
                }

                if(node.isLeaf()) {
                    break;
                }

                if(parent.getNode((i+1)&3)!=null || parent.getNode((i+2)&3)!=null || parent.getNode((i+3)&3)!=null) {
                    retainer = parent;
                    j = i;
                }
            }
        }

        // Find the point to remove.
        // This is done by .equals(), not by matching x,y,
        // so if there are multiple points with the same x,y,
        // the correct point is removed.
        //
        int toDelete = -1;
        for(int ix=0; ix<node.getData().size(); ix++) {
            if(node.getData().get(ix).equals(vx)) {
                toDelete = ix;
                break;
            }
        }

        if(toDelete==-1) {
            // Found a coincident point: same x,y, different object.
            // Therefore there's nothing to remove.
            //
            return this;
        } else {
            node.getData().remove(toDelete);

            // If there was more than one point here, that's it.
            //
            if(!node.getData().isEmpty()) {
                return this;
            }
        }

        // If this is the root point, remove it.
        //
        if(parent==null) {
            root = null;
            return this;
        }

        // Remove this leaf.
        //
        parent.getNodes()[i] = null;

        // If the parent now contains exactly one leaf, collapse superfluous parents.
        //
        int notNull = 0;
        for(int ix=0; ix<4; ix++) {
            if(parent.getNode(ix)!=null) {
                node = parent.getNode(ix);
                notNull++;
            }
        }

        if(notNull==1 && node.isLeaf()) {
            if(retainer!=null) {
                retainer.getNodes()[j] = node;
            } else {
                root = node;
            }
        }

        return this;
    }

    public D3QuadTree remove(final List<IPoint> vxs) {
        vxs.forEach(this::remove);

        return this;
    }

    public List<IPoint> getData() {
        class Data implements D3Visitor {
            final List<IPoint> data = new ArrayList<>();

            @Override
            public boolean callback(final D3Quad quad) {
                if(quad.node.isLeaf()) {
                    data.addAll(quad.node.getData());
                }

                return false;
            }
        }

        final Data d = new Data();
        visit(d);

        return Collections.unmodifiableList(d.data);
    }

    public int size() {
        class Counter implements D3Visitor {
            int count = 0;

            @Override
            public boolean callback(final D3Quad quad) {
                if(quad.node.isLeaf()) {
                    count += quad.node.getData().size();
                }

                return false;
            }
        }

        final Counter c = new Counter();
        visit(c);

        return c.count;
    }

    public D3QuadNode getRoot() {
        return root;
    }
}
