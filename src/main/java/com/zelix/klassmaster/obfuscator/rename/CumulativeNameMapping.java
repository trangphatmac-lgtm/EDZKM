package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class CumulativeNameMapping extends ComposableNameMapping {
    @Override
    public int composeWith(Map map1) {
        if (super.mapping == null) {
            super.mapping = ZkmUtils.copyToHashMap(map1);
        } else if (map1 != null && map1.size() > 0) {
            TwoKeyMap twoKeyMap = new TwoKeyMap(super.mapping.size());
            Iterator iterator = super.mapping.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                twoKeyMap.putValue(value, key, key);
            }

            iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry1 = (Entry) iterator.next();
                Object keyValue = entry1.getKey();
                Object valueValue = entry1.getValue();
                Map map2 = twoKeyMap.getInnerMap(keyValue);
                if (map2 != null) {
                    for (Object keyValue2 : map2.keySet()) {
                        super.mapping.put(keyValue2, valueValue);
                    }
                } else {
                    super.mapping.put(keyValue, valueValue);
                }
            }
        }

        return this.getMappingSize();
    }
}
