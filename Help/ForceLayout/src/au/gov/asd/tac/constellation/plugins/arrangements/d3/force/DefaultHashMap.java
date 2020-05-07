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

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A HashMap that automatically adds and returns a new instance of
 * its value class if the key in get() has no existing value.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author algol
 */
public class DefaultHashMap<K, V> extends HashMap<K, V> {

    private final Class<V> defaultClass;

    public DefaultHashMap(final Class<V> defaultClass) {
        this.defaultClass = defaultClass;
    }

    @Override
    public V get(final Object k) {
        if(!containsKey((K)k)) {
            try {
                super.put((K)k, defaultClass.getDeclaredConstructor().newInstance());
            } catch(final NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(DefaultHashMap.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return super.get(k);
    }
}