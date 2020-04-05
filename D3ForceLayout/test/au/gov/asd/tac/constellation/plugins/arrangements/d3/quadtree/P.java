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

/**
 * An implementation of IVertex sufficient for testing.
 *
 * @author algol
 */
public class P implements IPoint{
    private static int idCounter = 0;

    final int id;
    final double x;
    final double y;
    final double radius;

    public P(final double x, final double y) {
        this.id = nextId();
        this.x = x;
        this.y = y;
        this.radius = 1;
    }

    public P(final double x, final double y, final double radius) {
        this.id = nextId();
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    private static synchronized int nextId() {
        return idCounter++;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    public int getId() {
        return id;
    }

    public boolean equals(final double x, final double y) {
        return this.x==x && this.y==y;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final P other = (P)obj;
        return this.id == other.id;
    }

    public static P p(final double x, double y) {
        return new P(x, y);
    }

    /**
     * Do not change toString(), it is used for testing.
     *
     * @return
     */
    @Override
    public String toString() {
        return String.format("[%s,%s]", x, y);
    }
}
