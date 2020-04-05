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

import au.gov.asd.tac.constellation.plugins.arrangements.d3.quadtree.IPoint;

/**
 * Leaf nodes contain a list of these.
 * <p>
 * A list is guaranteed to have at least one IVertex. If there is more than
 * one, the x,y points are coincident.
 *
 * @author algol
 */
public interface IVertex extends IPoint {
    void setX(final double x);
    void setY(final double y);
    double getRadius();

    /**
     * A unique value for each vertex.
     *
     * @param index A unique value for each vertex.
     */
    void setIndex(final int index);

    /**
     * A unique value for each vertex.
     *
     * @return A unique value for each vertex.
     */
    int getIndex();

    double getXVelocity();
    double getYVelocity();
    void setXVelocity(final double xv);
    void setYVelocity(final double yv);
}
