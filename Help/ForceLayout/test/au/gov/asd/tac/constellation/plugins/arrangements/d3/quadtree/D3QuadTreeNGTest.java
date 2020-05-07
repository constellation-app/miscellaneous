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

import static au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.P.p;
import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertArrayEquals;
import org.testng.annotations.Test;

/**
 * Test suite.
 * <p>
 * The recursive Node.toString() method, plus V.toString(), provide a
 * reasonable representation of a Node tree. Therefore, rather than write
 * a separate deep comparison for Node instances, we just use the String
 * representation for comparing actual and expected values.
 *
 * @author algol
 */
public class D3QuadTreeNGTest {

    private static P xy(double x, double y) {
        return new P(x, y);
    }

    @Test(description = "cover(x, y) sets a trivial extent if the extent was undefined")
    public void coverTrivialExtent() {
        final double[] extent = new D3QuadTree().cover(1, 2).getExtent();
        assertArrayEquals(new double[]{1, 2, 2, 3}, extent, 0);
    }

    @Test(description = "cover(x, y) sets a non-trivial squarified and centered extent if the extent was trivial")
    public void coverCentredExtent() {
        final double[] extent = new D3QuadTree().cover(0, 0).cover(1, 2).getExtent();
        assertArrayEquals(new double[]{0, 0, 4, 4}, extent, 0);
    }

    @Test(description = "cover(x, y) ignores invalid points")
    public void coverInvalidPoint() {
        final double[] extent = new D3QuadTree().cover(0, 0).cover(Double.NaN, 2).getExtent();
        assertArrayEquals(new double[]{0, 0, 1, 1}, extent, 0);
    }

    @Test(description = "cover(x, y) repeatedly doubles the existing extent if the extent was non-trivial")
    public void coverExtentDoubles() {
        double[] extent;

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-1, -1).getExtent();
        assertArrayEquals(new double[]{-4, -4, 4, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(1, -1).getExtent();
        assertArrayEquals(new double[]{0, -4, 8, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(3, -1).getExtent();
        assertArrayEquals(new double[]{0, -4, 8, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(3, 1).getExtent();
        assertArrayEquals(new double[]{0, 0, 4, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(3, 3).getExtent();
        assertArrayEquals(new double[]{0, 0, 4, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(1, 3).getExtent();
        assertArrayEquals(new double[]{0, 0, 4, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-1, 3).getExtent();
        assertArrayEquals(new double[]{-4, 0, 4, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-1, 1).getExtent();
        assertArrayEquals(new double[]{-4, 0, 4, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-3, -3).getExtent();
        assertArrayEquals(new double[]{-4, -4, 4, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(3, -3).getExtent();
        assertArrayEquals(new double[]{0, -4, 8, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(5, -3).getExtent();
        assertArrayEquals(new double[]{0, -4, 8, 4}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(5, 3).getExtent();
        assertArrayEquals(new double[]{0, 0, 8, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(5, 5).getExtent();
        assertArrayEquals(new double[]{0, 0, 8, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(3, 5).getExtent();
        assertArrayEquals(new double[]{0, 0, 8, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-3, 5).getExtent();
        assertArrayEquals(new double[]{-4, 0, 4, 8}, extent, 0);

        extent = new D3QuadTree().cover(0, 0).cover(2, 2).cover(-3, 3).getExtent();
        assertArrayEquals(new double[]{-4, 0, 4, 8}, extent, 0);
    }

    @Test(description="cover(x, y) repeatedly wraps the root node if it has children")
    public void addWrapRootNode() {
        final D3QuadTree q = new D3QuadTree().add(new P(0, 0)).add(new P(2, 2));

        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[2.0,2.0]}]");
    }

    @Test(description="extent(extent) extends the extent")
    public void extent() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 1, 2, 6);
        assertArrayEquals(new double[]{0, 1, 8, 9}, q.getExtent(), 0);
    }

    @Test(description="extent() can be inferred by quadtree.cover()")
    public void extentCover() {
        final D3QuadTree q = new D3QuadTree();

        q.cover(0, 0);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q.getExtent(), 0);

        q.cover(2, 4);
        assertArrayEquals(new double[]{0, 0, 8, 8}, q.getExtent(), 0);
    }

    @Test(description="extent() can be inferred by quadtree.add()")
    public void extentAdd() {
        final D3QuadTree q = new D3QuadTree();

        q.add(xy(0, 0));
        assertArrayEquals(new double[]{0, 0, 1, 1}, q.getExtent(), 0);

        q.add(xy(2, 4));
        assertArrayEquals(new double[]{0, 0, 8, 8}, q.getExtent(), 0);
    }

    @Test(description="extent(extent) squarifies and centers the specified extent")
    public void extentCentre() {
        final D3QuadTree q = new D3QuadTree();

        q.setExtent(0, 1, 2, 6);
        assertArrayEquals(new double[]{0, 1, 8, 9}, q.getExtent(), 0);
    }

    @Test(description="extent(extent) ignores invalid extents")
    public void extentIgnoreInvalid() {
        final D3QuadTree q0 = new D3QuadTree().setExtent(1, Double.NaN, Double.NaN, 0);
        assertNull(q0.getExtent());
    }

    @Test(description="extent(extent) flips inverted extents")
    public void extentInverted() {
        final D3QuadTree q = new D3QuadTree().setExtent(1, 1, 0, 0);
        assertArrayEquals(new double[]{0, 0, 2, 2}, q.getExtent(), 0);
    }

    @Test(description="extent(extent) tolerates partially-valid extents")
    public void extentPartiallyValid() {
        final D3QuadTree q0 = new D3QuadTree().setExtent(Double.NaN, 0, 1, 1);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q0.getExtent(), 0);

        final D3QuadTree q1 = new D3QuadTree().setExtent(0, Double.NaN, 1, 1);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q1.getExtent(), 0);

        final D3QuadTree q2 = new D3QuadTree().setExtent(0, 0, Double.NaN, 1);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q2.getExtent(), 0);

        final D3QuadTree q3 = new D3QuadTree().setExtent(0, 0, 1, Double.NaN);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q3.getExtent(), 0);
    }

    @Test(description="extent(extent) allows trivial extents")
    public void extentTrivial() {
        final D3QuadTree q0 = new D3QuadTree().setExtent(0, 0, 0, 0);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q0.getExtent(), 0);

        final D3QuadTree q1 = new D3QuadTree().setExtent(1, 1, 1, 1);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q1.getExtent(), 0);
    }

    @Test(description = "add(datum) creates a new point and adds it to the quadtree")
    public void addPoint() {
        final D3QuadTree q = new D3QuadTree();

        q.add(new P(0.0, 0.0));
        assertEquals(q.getRoot().toString(), "{[0.0,0.0]}");

        q.add(new P(0.9, 0.9));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[0.9,0.9]}]");

        q.add(new P(0.9, 0.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[0.9,0.0]},,{[0.9,0.9]}]");

        q.add(new P(0, 0.9));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[0.9,0.0]},{[0.0,0.9]},{[0.9,0.9]}]");

        q.add(new P(0.4, 0.4));
        assertEquals(q.getRoot().toString(), "[[{[0.0,0.0]},,,{[0.4,0.4]}],{[0.9,0.0]},{[0.0,0.9]},{[0.9,0.9]}]");
    }

    @Test(description = "add(datum) handles points being on the perimeter of the quadtree bounds")
    public void addPointPerimeter() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 1, 1);

        q.add(new P(0.0, 0.0));
        assertEquals(q.getRoot().toString(), "{[0.0,0.0]}");

        q.add(new P(1.0, 1.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[1.0,1.0]}]");

        q.add(new P(1.0, 0.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[1.0,0.0]},,{[1.0,1.0]}]");

        q.add(new P(0.0, 1.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[1.0,0.0]},{[0.0,1.0]},{[1.0,1.0]}]");
    }

    @Test(description = "add(datum) handles points being to the top of the quadtree bounds")
    public void addPointBottom() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 2, 2);
        q.add(new P(1.0, -1.0));
        assertArrayEquals(new double[]{0, -4, 8, 4}, q.getExtent(), 0);
    }

    @Test(description = "add(datum) handles points being to the right of the quadtree bounds")
    public void addPointRight() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 2, 2);
        q.add(new P(3.0, 1.0));
        assertArrayEquals(new double[]{0, 0, 4, 4}, q.getExtent(), 0);
    }

    @Test(description = "add(datum) handles points being to the bottom of the quadtree bounds")
    public void addPointTop() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 2, 2);
        q.add(new P(1.0, 3.0));
        assertArrayEquals(new double[]{0, 0, 4, 4}, q.getExtent(), 0);
    }

    @Test(description = "add(datum) handles points being to the left of the quadtree bounds")
    public void addPointLeft() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 2, 2);
        q.add(new P(-1.0, 1.0));
        assertArrayEquals(new double[]{-4, 0, 4, 8}, q.getExtent(), 0);
    }

    @Test(description = "add(datum) handles coincident points")
    public void addPointCoincident() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 1, 1);

        q.add(new P(0.0, 0.0));
        assertEquals(q.getRoot().toString(), "{[0.0,0.0]}");

        q.add(new P(1.0, 0.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[1.0,0.0]},,]");

        q.add(new P(0.0, 1.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[1.0,0.0]},{[0.0,1.0]},]");

        q.add(new P(0.0, 1.0));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},{[1.0,0.0]},{[0.0,1.0],[0.0,1.0]},]");
    }

    @Test(description="add many coincident points")
    public void addPointManyCoincident() {
        final D3QuadTree q = new D3QuadTree();

        final List<String> points = new ArrayList<>();
        for(int i=0; i<100; i++) {
            final IPoint p = p(0, 0);
            q.add(p);
            points.add(p.toString());
        }

        final String expected = "{" + String.join(",", points) + "}";
        assertEquals(q.getRoot().toString(), expected);
    }

    @Test(description = "add(datum) implicitly defines trivial bounds for the first point")
    public void addTrivialBounds() {
        final D3QuadTree q = new D3QuadTree().add(new P(1, 2));

        assertArrayEquals(new double[]{1, 2, 2, 3}, q.getExtent(), 0);
        assertEquals(q.getRoot().toString(), "{[1.0,2.0]}");
    }

    @Test(description="addAll(data) ignores points with NaN coordinates")
    public void addAllNaN() {
        final D3QuadTree q = new D3QuadTree();

        q.add(List.of(xy(Double.NaN, 0), xy(0, Double.NaN)));
        assertNull(q.getRoot());

        q.add(List.of(xy(0, 0), xy(0.9, 0.9)));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[0.9,0.9]}]");

        q.add(List.of(xy(Double.NaN, 0), xy(0, Double.NaN)));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[0.9,0.9]}]");
        assertArrayEquals(new double[]{0, 0, 1, 1}, q.getExtent(), 0);
    }

    @Test(description="addAll(data) correctly handles the empty array")
    public void addAllEmpty() {
        final D3QuadTree q = new D3QuadTree();

        q.add(List.of());
        assertNull(q.getExtent());

        q.add(List.of(xy(0, 0), xy(1, 1)));
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[1.0,1.0]}]");

        q.add(List.of());
        assertEquals(q.getRoot().toString(), "[{[0.0,0.0]},,,{[1.0,1.0]}]");
        assertArrayEquals(new double[]{0, 0, 2, 2}, q.getExtent(), 0);
    }

    @Test(description="addAll(data) computes the extent of the data before adding")
    public void addAllComputeExtent() {
        final D3QuadTree q = new D3QuadTree();
        q.add(List.of(xy(0.4, 0.4), xy(0, 0), xy(0.9, 0.9)));
        assertEquals(q.getRoot().toString(), "[[{[0.0,0.0]},,,{[0.4,0.4]}],,,{[0.9,0.9]}]");
    }

    @Test(description="add vd addAll: addAll trees are probably more compact")
    public void addSingleVsAll() {
        // Adding a list results in a more compact quadtree because
        // the extent of the data is computed first before adding the data.
        //
        final List<IPoint> points = new ArrayList();
        for(int i=0; i<6; i++) {
            points.add(p(i, i));
        }
        final D3QuadTree q0 = new D3QuadTree().add(points);

        final D3QuadTree q1 = new D3QuadTree();
        points.forEach(q1::add);
        assertNotEquals(q0.getRoot().toString(), q1.getRoot().toString());
    }

    @Test(description="find(x, y) returns the closest point to the given [x, y]")
    public void findClosestPoint() {
        final int dx = 17;
        final int dy = 17;
        final D3QuadTree q = new D3QuadTree();
        for(int i=0; i<dx*dy; i++) {
            q.add(xy(i%dx, i/dy));
        }

        final P p0 = (P)q.find(0.1, 0.1);
        assertTrue(p0.equals(0, 0));

        final P p1 = (P)q.find(7.1, 7.1);
        assertTrue(p1.equals(7, 7));

        final P p2 = (P)q.find(0.1, 15.9);
        assertTrue(p2.equals(0, 16));

        final P p3 = (P)q.find(15.9, 15.9);
        assertTrue(p3.equals(16, 16));
    }

    @Test(description="find(x, y, radius) returns the closest point within the search radius to the given [x, y]")
    public void findClosestPointInRadius() {
        final D3QuadTree q = new D3QuadTree();
        q.add(List.of(xy(0, 0), xy(100, 0), xy(0, 100), xy(100, 100)));

        final P p0 = (P)q.find(20, 20);
        assertTrue(p0.equals(0, 0));

        final P p1 = (P)q.find(20, 20, 20 * Math.sqrt(2) + 1e6);
        assertTrue(p1.equals(0, 0));

        final P p2 = (P)q.find(20, 20, 20 * Math.sqrt(2) - 1e6);
        assertNull(p2);

        final P p3 = (P)q.find(0, 20, 20 + 1e6);
        assertTrue(p3.equals(0, 0));

        final P p4 = (P)q.find(0, 20, 20 - 1e6);
        assertNull(p4);

        final P p5 = (P)q.find(20, 0, 20 + 1e6);
        assertTrue(p5.equals(0, 0));

        final P p6 = (P)q.find(20, 0, 20 - 1e6);
        assertNull(p6);
    }

    @Test(description="find(x, y, Inifinity) works")
    public void findInfinity() {
        final D3QuadTree q = new D3QuadTree();
        q.add(List.of(xy(0, 0), xy(100, 0), xy(0, 100), xy(100, 100)));

        final P p0 = (P)q.find(20, 20, Double.POSITIVE_INFINITY);
        assertTrue(p0.equals(0, 0));

        final P p1 = (P)q.find(2000, 2000, Double.POSITIVE_INFINITY);
        assertTrue(p1.equals(100, 100));

        final P p3 = (P)q.find(-2000, -2000, Double.POSITIVE_INFINITY);
        assertTrue(p3.equals(0, 0));
    }

    @Test(description="visit(callback) visits each node in a quadtree")
    public void visitQuadtree() {
        final List<IPoint> vxs = List.of(new P(0, 0), new P(1, 0), new P(0, 1), new P(1, 1));
        final D3QuadTree q = new D3QuadTree().add(vxs);
        final VisitTester vt = new VisitTester();
        q.visit(vt);
        final String expected = "[0.0,0.0,2.0,2.0],[0.0,0.0,1.0,1.0],[1.0,0.0,2.0,1.0],[0.0,1.0,1.0,2.0],[1.0,1.0,2.0,2.0]";
        assertEquals(String.join(",", vt.quadStrings), expected);
    }

    @Test(description="visit(callback) applies pre-order traversal")
    public void visitPreorderTraversal() {
        final List<IPoint> vxs = List.of(new P(100, 100), new P(200, 200), new P(300, 300));
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 960, 960).add(vxs);
        final VisitTester vt = new VisitTester();
        q.visit(vt);
        final String expected = "[0.0,0.0,1024.0,1024.0],[0.0,0.0,512.0,512.0],[0.0,0.0,256.0,256.0],[0.0,0.0,128.0,128.0],[128.0,128.0,256.0,256.0],[256.0,256.0,512.0,512.0]";
        assertEquals(String.join(",", vt.quadStrings), expected);
    }

    @Test(description="visit(callback) does not recurse if the callback returns true")
    public void visitPartialRecurse() {
        final List<IPoint> vxs = List.of(new P(100, 100), new P(700, 700), new P(800, 800));
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 960, 960).add(vxs);
        final VisitTesterPartial vt = new VisitTesterPartial();
        q.visit(vt);
        final String expected = "[0.0,0.0,1024.0,1024.0],[0.0,0.0,512.0,512.0],[512.0,512.0,1024.0,1024.0]";
        assertEquals(String.join(",", vt.quadStrings), expected);
    }

    @Test(description="visit(callback) on an empty quadtree with no bounds does nothing")
    public void visitEmptyQuadtree() {
        final D3QuadTree q = new D3QuadTree();
        final VisitTester vt = new VisitTester();
        q.visit(vt);
        assertEquals(vt.quadStrings.size(), 0);
    }

    @Test(description="visit(callback) on an empty quadtree with bounds does nothing")
    public void visitEmptyQuadtreeExtents() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 960, 960);
        final VisitTester vt = new VisitTester();
        q.visit(vt);
        assertEquals(vt.quadStrings.size(), 0);
    }

    private static class VisitTester implements D3Visitor {
        final List<String> quadStrings = new ArrayList<>();
        @Override
        public boolean callback(final D3Quad quad) {
            quadStrings.add(String.format("[%s,%s,%s,%s]", quad.x0, quad.y0, quad.x1, quad.y1));

            return false;
        }
    }

    private static class VisitTesterPartial implements D3Visitor {
        final List<String> quadStrings = new ArrayList<>();
        @Override
        public boolean callback(final D3Quad quad) {
            quadStrings.add(String.format("[%s,%s,%s,%s]", quad.x0, quad.y0, quad.x1, quad.y1));

            return quad.x0>0;
        }
    }

    @Test(description="visitAfter")
    public void visitAfter() {
        final List<IPoint> points = new ArrayList();
        for(int i=0; i<6; i++) {
            points.add(p(i, i));
        }
        final D3QuadTree q = new D3QuadTree().add(points);

        q.visitAfter(new VisitAfter());
    }

    private static class VisitAfter implements D3AfterVisitor {
        @Override
        public void callback(final D3Quad quad) {
//            System.out.printf("quad %s\n", quad);
        }

    }

    @Test(description="copy() returns a copy of this quadtree")
    public void copy() {
        final D3QuadTree q0 = new D3QuadTree().add(List.of(xy(0, 0), xy(1,0), xy(0, 1), xy(1, 1)));
        final D3QuadTree q1 = q0.copy();
        assertEquals(q1.getRoot().toString(), q0.getRoot().toString());
    }

    @Test(description="copy() isolates changes to the extent")
    public void copyExtentIsolated() {
        final D3QuadTree q0 = new D3QuadTree().setExtent(0, 0, 1, 1);
        final D3QuadTree q1 = q0.copy();
        q0.add(xy(2, 2));
        assertArrayEquals(new double[]{0, 0, 2, 2}, q1.getExtent(), 0);
        q1.add(xy(-1, -1));
        assertArrayEquals(new double[]{0, 0, 4, 4}, q0.getExtent(), 0);
    }

    @Test(description="copy() isolates changes to the root when a leaf")
    public void copyRootIsolatedWhenALeaf() {
        final D3QuadTree q0 = new D3QuadTree().setExtent(0, 0, 1, 1);
        D3QuadTree q1 = q0.copy();
        final P p0 = xy(2, 2);
        q0.add(p0);
        assertNull(q1.getRoot());

        q1 = q0.copy();
        assertEquals(q0.getRoot().toString(), "{[2.0,2.0]}");
        assertEquals(q1.getRoot().toString(), "{[2.0,2.0]}");

        q0.remove(p0);
        assertNull(q0.getRoot());
        assertEquals(q1.getRoot().toString(), "{[2.0,2.0]}");
    }

    @Test(description="copy() isolates changes to the root when not a leaf")
    public void copyRootIsolatedWhenNotALeaf() {
        final P p0 = xy(1, 1);
        final P p1 = xy(2, 2);
        final P p2 = xy(3, 3);
        final D3QuadTree q0 = new D3QuadTree().setExtent(0, 0, 4, 4).add(List.of(p0, p1));
        D3QuadTree q1 = q0.copy();
        q0.add(p2);
        assertArrayEquals(new double[]{0, 0, 8, 8}, q0.getExtent(), 0);
        assertEquals(q0.getRoot().toString(), "[[{[1.0,1.0]},,,[{[2.0,2.0]},,,{[3.0,3.0]}]],,,]");
        assertArrayEquals(new double[]{0, 0, 8, 8}, q1.getExtent(), 0);
        assertEquals(q1.getRoot().toString(), "[[{[1.0,1.0]},,,{[2.0,2.0]}],,,]");

        q1 = q0.copy();
        q0.remove(p2);
        assertArrayEquals(new double[]{0, 0, 8, 8}, q1.getExtent(), 0);
        assertEquals(q1.getRoot().toString(), "[[{[1.0,1.0]},,,[{[2.0,2.0]},,,{[3.0,3.0]}]],,,]");
    }

    @Test(description="remove(datum) removes a point and returns the quadtree")
    public void remove() {
        final P p0 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(p0);
        assertEquals(q.getRoot().toString(), "{[1.0,1.0]}");
        q.remove(p0);
        assertNull(q.getRoot());
    }

    @Test(description="remove(datum) removes the only point in the quadtree")
    public void removeOnlyPoint() {
        final P p0 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(p0);
        q.remove(p0);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q.getExtent(), 0);
        assertNull(q.getRoot());
        assertEquals(p0.toString(), "[1.0,1.0]");
    }

    @Test(description="remove(datum) removes a first coincident point at the root in the quadtree")
    public void removeCoincidentPoint() {
        final P p0 = xy(1, 1);
        final P p1 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(List.of(p0, p1));
        q.remove(p0);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q.getExtent(), 0);
        assertEquals(q.getRoot().toString(), "{[1.0,1.0]}");
        assertEquals(((P)q.getRoot().getData().get(0)).getId(), p1.getId());
        assertEquals(p0.toString(), "[1.0,1.0]");
        assertEquals(p1.toString(), "[1.0,1.0]");
    }

    @Test(description="remove(datum) removes another coincident point at the root in the quadtree")
    public void removeAnotherCoincidentPoint() {
        final P p0 = xy(1, 1);
        final P p1 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(List.of(p0, p1));
        q.remove(p1);
        assertArrayEquals(new double[]{1, 1, 2, 2}, q.getExtent(), 0);
        assertEquals(q.getRoot().toString(), "{[1.0,1.0]}");
        assertEquals(List.of(p0), q.getRoot().getData());
        assertEquals(p0.toString(), "[1.0,1.0]");
        assertEquals(p1.toString(), "[1.0,1.0]");
    }

    @Test(description="remove(datum) removes a non-root point in the quadtree")
    public void removeNonRootPoint() {
        final P p0 = xy(0, 0);
        final P p1 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(List.of(p0, p1));
        q.remove(p0);
        assertArrayEquals(new double[]{0, 0, 2, 2}, q.getExtent(), 0);
        assertEquals(q.getRoot().getData(), List.of(p1));
        assertEquals(p0.toString(), "[0.0,0.0]");
        assertEquals(p1.toString(), "[1.0,1.0]");
    }

    @Test(description="remove(datum) removes another non-root point in the quadtree")
    public void removeAnotherNonRootPoint() {
        final P p0 = xy(0, 0);
        final P p1 = xy(1, 1);
        final D3QuadTree q = new D3QuadTree().add(List.of(p0, p1));
        q.remove(p1);
        assertArrayEquals(new double[]{0, 0, 2, 2}, q.getExtent(), 0);
        assertEquals(q.getRoot().getData(), List.of(p0));
        assertEquals(p0.toString(), "[0.0,0.0]");
        assertEquals(p1.toString(), "[1.0,1.0]");
    }

    @Test(description="remove(datum) ignores a point not in the quadtree")
    public void removeIgnoresNonexistentPoint() {
        final P p0 = xy(0, 0);
        final P p1 = xy(1, 1);
        final D3QuadTree q0 = new D3QuadTree().add(p0);
        final D3QuadTree q1 = new D3QuadTree().add(p1);
        q0.remove(p1);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q0.getExtent(), 0);
        assertEquals(q0.getRoot().getData(), List.of(p0));
        assertEquals(q1.getRoot().getData(), List.of(p1));
    }

    @Test(description="remove(datum) ignores a coincident point not in the quadtree")
    public void removeIgnoresNonexistentCoincidentPoint() {
        final P p0 = xy(0, 0);
        final P p1 = xy(0, 0);
        final D3QuadTree q0 = new D3QuadTree().add(p0);
        final D3QuadTree q1 = new D3QuadTree().add(p1);
        assertEquals(q0.getRoot().getData(), List.of(p0));
        assertEquals(q1.getRoot().getData(), List.of(p1));
        q0.remove(p1);
        assertArrayEquals(new double[]{0, 0, 1, 1}, q0.getExtent(), 0);
        assertEquals(q0.getRoot().getData(), List.of(p0));
        assertEquals(q1.getRoot().getData(), List.of(p1));
    }

    @Test(description="remove(datum) removes another point in the quadtree")
    public void removeAnotherPoint() {
        final D3QuadTree q = new D3QuadTree().setExtent(0, 0, 959, 959)
                .add(List.of(
                xy(630, 438), xy(715, 464), xy(523, 519), xy(646, 318), xy(434, 620), xy(570, 489), xy(520, 345), xy(459, 443), xy(346, 405), xy(529, 444)
        ));

        q.remove(q.find(546, 440));
        assertArrayEquals(new double[]{0, 0, 1024, 1024}, q.getExtent(), 0);
        final String expected = "[[,,,[,,{[346.0,405.0]},{[459.0,443.0]}]],[,,[{[520.0,345.0]},{[646.0,318.0]},[,{[630.0,438.0]},{[570.0,489.0]},],{[715.0,464.0]}],],{[434.0,620.0]},{[523.0,519.0]}]";
        assertEquals(q.getRoot().toString(), expected);
    }

    @Test(description="size() returns the number of points in the quadtree")
    public void size() {
        final D3QuadTree q = new D3QuadTree();
        assertEquals(q.size(), 0);

        q.add(xy(0, 0)).add(xy(1, 2));
        assertEquals(q.size(), 2);
    }

    @Test(description="size() correctly counts coincident nodes")
    public void sizeCoincident() {
        final D3QuadTree q = new D3QuadTree();

        q.add(xy(0, 0)).add(xy(0, 0));
        assertEquals(q.size(), 2);
    }

    @Test(description="data() returns an array of data in the quadtree")
    public void data() {
        final D3QuadTree q = new D3QuadTree();
        assertEquals(q.getData(), List.of());

        final P p0 = xy(0, 0);
        final P p1 = xy(1, 2);
        q.add(p0).add(p1);
        assertEquals(q.getData(), List.of(p0, p1));
    }

    @Test(description="data() correctly handles coincident nodes")
    public void dataCoincident() {
        final D3QuadTree q = new D3QuadTree();

        final P p0 = xy(0, 0);
        final P p1 = xy(0, 0);
        q.add(p0).add(p1);
        assertEquals(q.getData(), List.of(p0, p1));
    }
}
