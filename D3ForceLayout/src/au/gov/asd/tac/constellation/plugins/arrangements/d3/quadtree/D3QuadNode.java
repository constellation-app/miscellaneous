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
import java.util.List;
import java.util.stream.Collectors;

/**
 *An internal node of a D3QuadTree.
 * <p>
 * Internal nodes of the quadtree are represented as four-element arrays in
 * left-to-right, top-to-bottom order:
 * <ul>
 *   <li>0 - the top-left quadrant, if any.</li>
 *   <li>1 - the top-right quadrant, if any.</li>
 *   <li>2 - the bottom-left quadrant, if any.</li>
 *   <li>3 - the bottom-right quadrant, if any.</li>
 * </ul>
 *
 * A child quadrant may be undefined if it is null.
 * <p>
 * Leaf nodes are represented as objects with the following properties:
 * <ul>
 *   <li>data - a list of points, as passed to D3QuadTree.add(). There is more
 *      than one point if the x,y coordinates are the same.</li>
 *   <li>nodes is null</li>
 * </ul>

 * @author algol
 */
public class D3QuadNode {
    public static final int NQUADS = 4;

    private static int idCounter = 0;

    /**
     * Use this as a key for related data.
     */
    public final int id;

    private final D3QuadNode[] nodes;
    private final List<IPoint> data;

    /**
     * An arbitrary value to be used by anything.
     */
    private Object value;

    public D3QuadNode() {
        this.id = nextId();
        this.nodes = new D3QuadNode[NQUADS];
        this.data = null;
        value = null;
    }

    public D3QuadNode(final IPoint vx) {
        this.id = nextId();
        nodes = null;
        data = new ArrayList<>();
        data.add(vx);
        value = null;
    }

    public D3QuadNode(final D3QuadNode other) {
        this.id = nextId();
        if(other.nodes!=null) {
            nodes = new D3QuadNode[NQUADS];
            System.arraycopy(other.nodes, 0, nodes, 0, NQUADS);
        } else {
            nodes = null;
        }

        if(other.data!=null) {
            data = new ArrayList(other.data);
        } else {
            data = null;
        }

        value = null;
    }

    private static synchronized int nextId() {
        return idCounter++;
    }

    public D3QuadNode[] getNodes() {
        if(isLeaf()) {
            throw new IllegalStateException("Is a leaf");
        }

        return nodes;
    }

    public D3QuadNode getNode(final int ix) {
        if(isLeaf()) {
            throw new IllegalStateException("Is a leaf");
        }

        return nodes[ix];
    }

    /**
     * Return the list of IPoints in this leaf node.
     * <p>
     * Remember that there is always at least one element in the list
     * if this is a leaf node. If there is more than one element, they
     * all have same x,y position.
     *
     * @return A list of one or more coincident IPoints.
     */
    public List<IPoint> getData() {
        if(!isLeaf()) {
            throw new IllegalStateException("Not a leaf");
        }

        return data;
    }

    public D3QuadNode copyLeaf() {
        if(!isLeaf()) {
            throw new IllegalStateException("Not a leaf");
        }

        return new D3QuadNode(this);
    }

    public boolean isLeaf() {
        return data!=null;
    }

    public D3QuadNode clearNodes() {
        if(isLeaf()) {
            throw new IllegalStateException("Is a leaf");
        }

        for(int ix=0; ix<NQUADS; ix++) {
            nodes[ix] = null;
        }

        return this;
    }

//    public float getX() {
//        return x;
//    }
//
//    public void setX(final float x) {
//        this.x = x;
//    }
//
//    public float getY() {
//        return y;
//    }
//
//    public void setY(final float y) {
//        this.y = y;
//    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    /**
     * Do not change the String output - it is used for testing.
     * <p>
     * It's much easier to output a String in the right format than it is to
     * recursively check for equality in tests.
     *
     * @return
     */
    @Override
    public String toString() {
        if(isLeaf()) {
            return "{" + data.stream().map(IPoint::toString).collect(Collectors.joining(",")) + "}";
        } else {
            return "[" + Arrays.stream(nodes).map(node -> node!=null?node.toString():"").collect(Collectors.joining(",")) + "]";
        }
    }
}
