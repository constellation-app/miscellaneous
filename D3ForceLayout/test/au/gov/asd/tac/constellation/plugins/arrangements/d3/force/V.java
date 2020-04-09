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

/**
 *
 * @author algol
 */
public class V implements IVertex {
    private double x;
    private double y;
    private final double radius;
    private int index;

    private double xVelocity;
    private double yVelocity;

    private String label;

    public V(final double x, final double y) {
        this(x, y, 1f);
    }

    public V(final double x, final double y, final double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        xVelocity = 0;
        yVelocity = 0;

        label = null;
    }

    @Override
    public void setX(final double x) {
        this.x = x;
    }

    @Override
    public void setY(final double y) {
        this.y = y;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setIndex(final int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public double getXVelocity() {
        return xVelocity;
    }

    @Override
    public double getYVelocity() {
        return yVelocity;
    }

    @Override
    public void setXVelocity(double vx) {
        this.xVelocity = vx;
    }

    @Override
    public void setYVelocity(double vy) {
        this.yVelocity = vy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public static V v(final double x, double y) {
        return new V(x, y);
    }

    @Override
    public String toString() {
        return String.format("[%s,%s,%s]", x, y, radius);
    }
}
