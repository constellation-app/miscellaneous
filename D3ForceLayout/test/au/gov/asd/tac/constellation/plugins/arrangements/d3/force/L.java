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
 * An implementation of ILink sufficient for unit testing.
 * 
 * @author algol
 */
public class L implements ILink {
    private final IVertex source;
    private final IVertex target;
    private int index;

    public L(final IVertex source, final IVertex target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public IVertex getSource() {
        return source;
    }

    @Override
    public IVertex getTarget() {
        return target;
    }

    @Override
    public void setIndex(final int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public static L l(final IVertex source, final IVertex target) {
        return new L(source, target);
    }

    @Override
    public String toString() {
        return String.format("[%s->%s]", ((V)source).getLabel(), ((V)target).getLabel());
    }
}
