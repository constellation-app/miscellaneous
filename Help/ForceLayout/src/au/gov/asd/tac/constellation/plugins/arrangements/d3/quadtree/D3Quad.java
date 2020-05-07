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
 *
 * @author algol
 */
public class D3Quad {
    public final D3QuadNode node;
    public final double x0;
    public final double y0;
    public final double x1;
    public final double y1;

    public D3Quad(final D3QuadNode node, final double x0, final double y0, final double x1, final double y1) {
        this.node = node;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    @Override
    public String toString() {
        return String.format("[%S,%s,%s,%s,%s", node, x0, y0, x1, y1);
    }
}
