package com.zelix.klassmaster.obfuscator.parameters;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.IdentityHashSet;
import com.zelix.klassmaster.util.IdentityMapWrapper;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.MutableLong;
import com.zelix.klassmaster.util.MutablePair;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.PairMultiMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;

public class ParameterKeyGenerator {
    public static int mergeSortDepthLimit;
    public static Iterator indexRandoms;
    public int keyCount;
    public Iterator randomLongs;
    public Set targetNodes;
    public int indexBitOffset;
    public Set usedMiddleValues;
    public Set sourceOnlyLayouts;
    public List allKeyNodes;
    public Set pendingLayoutNodes;
    public int rootPairCount;
    public int initialNodeCount;
    public int minLinkCount;
    public List initialNodes;
    public Set targetOnlyLayouts;
    public Set sourceNodes;
    public final List bitLayouts = new ArrayList();
    public final List sharedKeyPairs = new ArrayList();
    public boolean initialized = false;
    public Random random;
    public final Iterator middleValueIterator;
    public final ListMultimap nodesByLayout;
    public final Set layouts;
    public final RootKeyNode rootNode;

    public void generateInitialNodes(List list1, int ba, boolean bl) {
        boolean largeParameterKeys = false;
        this.initialNodes = new ArrayList(this.initialNodeCount);
        this.allKeyNodes = new ArrayList(this.keyCount);
        int[] layoutValue = MethodParameterChanger.createBitPermutation(this.random);
        this.addLayout(layoutValue);
        list1.add(this.getCurrentLayout().clone());
        int bc = 64 * (ba < 3010 ? 1 : 4);
        boolean bl1 = false;
        int bd = -1;
        if (HiddenOptionFlags.LARGE_PARAMETER_KEYS && bl) {
            int be = this.initialNodeCount * 17;
            int bf = Math.min(be, 65035);
            bd = Math.max(200 + this.random.nextInt(100), bf / 34 + this.random.nextInt(100));
        }

        int bn = 0;
        int bq = bn;

        for (int initialNodeCountValue = this.initialNodeCount; bq < initialNodeCountValue; initialNodeCountValue = this.initialNodeCount) {
            int bh = this.random.nextInt(256);
            MutablePair mutablePair = null;
            int bg;
            long mixedKeyValue;
            if (this.initialNodes.size() > 1 && this.random.nextInt(bc) == 0) {
                int elementIndex = this.random.nextInt(this.initialNodes.size());
                LongKeyNode longKeyNode = (LongKeyNode) this.initialNodes.get(elementIndex);
                mutablePair = new MutablePair(longKeyNode);
                mixedKeyValue = longKeyNode.getMixedKey();
                bg = longKeyNode.hashCode();
            } else {
                mixedKeyValue = this.nextMiddleValue();
                bg = this.random.nextInt(256);
            }

            LongKeyNode longKeyNode1 = new LongKeyNode(bh, mixedKeyValue, bg, this.getCurrentLayout(), this.nodesByLayout, this.layouts);
            list1.add(longKeyNode1.getCurrentKey());
            this.initialNodes.add(longKeyNode1);
            if (mutablePair != null) {
                mutablePair.setValue(longKeyNode1);
                this.sharedKeyPairs.add(mutablePair);
            }

            List list2 = this.nodesByLayout.getValues(layoutValue);
            int layoutNodeCount = list2 != null ? this.nodesByLayout.getValues(layoutValue).size() : 0;
            if (!(layoutNodeCount < 64 || this.random.nextInt(128) != 0)
                    || layoutNodeCount > 153 && this.random.nextInt(32) == 0) {
                layoutValue = layoutValue.clone();
                this.addLayout(layoutValue);
                list1.add(this.getCurrentLayout().clone());
            }
            largeParameterKeys = HiddenOptionFlags.LARGE_PARAMETER_KEYS;

            if (largeParameterKeys && bl && !bl1 && bn >= bd) {
                bl1 = true;
                long[] longArray = new long[100];
                int elementIndexValue = 0;
                Iterator iterator = list1.iterator();

                while (iterator.hasNext()) {
                    if (iterator.next() instanceof Long) {
                        LongKeyNode longKeyNode2 = (LongKeyNode) this.initialNodes.get(elementIndexValue);
                        longArray[elementIndexValue] = ((LongKeyNode) this.initialNodes.get(elementIndexValue)).extractBits();
                        elementIndexValue++;
                    }

                    if (elementIndexValue > 99) {
                        break;
                    }
                }

                list1.add(longArray);
            }

            bq = ++bn;
        }

        this.allKeyNodes.addAll(this.initialNodes);
        layoutValue = MethodParameterChanger.createBitPermutation(this.random);
        this.addLayout(layoutValue);
        list1.add(this.getCurrentLayout().clone());
        sortNodes(this.allKeyNodes);
        bn = 0;
        Iterator iterator1 = this.allKeyNodes.iterator();

        while (iterator1.hasNext()) {
            ((LongKeyNode) iterator1.next()).setIndex(bn++);
        }
    }

    public long encodeIndexKey(int nodeIndex) {
        int[] currentLayout = this.getCurrentLayout();
        int bc = 64 - this.indexBitOffset;
        long bd = (Long) indexRandoms.next();
        return MethodParameterChanger.composeIndexKey(nodeIndex, bc, bd, currentLayout);
    }

    public int[] getCurrentLayout() {
        return (int[]) this.bitLayouts.get(this.bitLayouts.size() - 1);
    }

    public long nextMiddleValue() {
        return (Long) this.middleValueIterator.next();
    }

    public int getLayoutGroupSize() {
        return 128;
    }

    public void classifyNodes(PairMultiMap pairMultiMap) {
        Map map1 = this.rootNode.getKeyChainMap();
        this.sourceNodes = new IdentityHashSet(ZkmUtils.getPrimeCapacity(map1.size()));
        this.targetNodes = new IdentityHashSet(ZkmUtils.getPrimeCapacity(map1.size()));
        ListMultimap listMultimap = new ListMultimap(true, map1.size());
        Iterator iterator = map1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            EncryptionKeyChain encryptionKeyChain = (EncryptionKeyChain) entry.getKey();
            EncryptionKeyChain encryptionKeyChain1 = (EncryptionKeyChain) entry.getValue();
            this.sourceNodes.add((LongKeyNode) encryptionKeyChain);
            if (encryptionKeyChain1.isKeyed()) {
                this.targetNodes.add((LongKeyNode) encryptionKeyChain1);
            }

            listMultimap.addValue(encryptionKeyChain1, (LongKeyNode) encryptionKeyChain);
        }

        IdentityHashSet identityHashSet = new IdentityHashSet(this.sourceNodes.size());
        Iterator iterator3 = this.sourceNodes.iterator();

        while (iterator3.hasNext()) {
            EncryptionKeyChain encryptionKeyChain2 = (EncryptionKeyChain) iterator3.next();
            if (!this.targetNodes.contains(encryptionKeyChain2)) {
                identityHashSet.add(encryptionKeyChain2);
            }
        }

        IdentityHashSet identityHashSet1 = new IdentityHashSet(this.targetNodes.size());
        Iterator iterator4 = this.targetNodes.iterator();

        while (iterator4.hasNext()) {
            EncryptionKeyChain encryptionKeyChain3 = (EncryptionKeyChain) iterator4.next();
            if (!this.sourceNodes.contains(encryptionKeyChain3)) {
                identityHashSet1.add(encryptionKeyChain3);
            }
        }

        IdentityHashSet identityHashSet2 = new IdentityHashSet(this.sharedKeyPairs.size() * 2);
        Iterator iterator5 = this.sharedKeyPairs.iterator();

        while (iterator5.hasNext()) {
            MutablePair mutablePair = (MutablePair) iterator5.next();
            identityHashSet2.add(mutablePair.getKey());
            identityHashSet2.add(mutablePair.getValue());
        }

        IdentityHashSet identityHashSet3 = new IdentityHashSet();
        IdentityHashSet identityHashSet4 = new IdentityHashSet();
        Iterator iterator1 = pairMultiMap.entrySet().iterator();

        while (iterator1.hasNext()) {
            Entry entry1 = (Entry) iterator1.next();
            String key = (String) entry1.getKey();
            List list1 = (List) entry1.getValue();
            int list1Size = list1.size();
            if (list1Size > 1) {
                identityHashSet4.clear();
                LongKeyNode longKeyNode = null;

                for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
                    ObjectPair objectPair = (ObjectPair) list1.get(elementIndex);
                    LongKeyNode longKeyNode1 = (LongKeyNode) objectPair.getFirst();
                    if (elementIndex == 0) {
                        longKeyNode = longKeyNode1;
                    }

                    if (elementIndex > 0 && longKeyNode1 != longKeyNode) {
                        identityHashSet3.add(longKeyNode1);
                    }

                    if (identityHashSet2.contains(longKeyNode1)) {
                        identityHashSet4.add(longKeyNode1);
                    }
                }

                if (identityHashSet4.size() > 1) {
                }
            }
        }

        int bc = 0;
        StringBuilder stringBuilder = new StringBuilder();
        Iterator iterator2 = this.layouts.iterator();

        while (iterator2.hasNext()) {
            int[] keyValue = (int[]) iterator2.next();
            List list2 = this.nodesByLayout.getValues(keyValue);
            stringBuilder.append("#" + bc++ + " (" + System.identityHashCode(keyValue) + ")\t" + list2.size());
            stringBuilder.append(ZkmAssert.lineSeparator);
        }

        this.sourceOnlyLayouts = new IdentityHashSet(this.nodesByLayout.getKeyCount());
        this.targetOnlyLayouts = new IdentityHashSet(this.nodesByLayout.getKeyCount());
        iterator2 = this.layouts.iterator();

        while (iterator2.hasNext()) {
            int[] keyValue2 = (int[]) iterator2.next();
            List list3 = this.nodesByLayout.getValues(keyValue2);
            boolean bl = false;
            boolean bl1 = false;
            Iterator iterator6 = list3.iterator();

            while (iterator6.hasNext()) {
                LongKeyNode longKeyNode2 = (LongKeyNode) iterator6.next();
                Set set1;
                if (this.sourceNodes.contains(longKeyNode2)) {
                    bl = true;
                    set1 = this.targetNodes;
                } else {
                    set1 = this.targetNodes;
                }

                if (set1.contains(longKeyNode2)) {
                    bl1 = true;
                }

                if (bl && bl1) {
                    break;
                }
            }

            if (bl && !bl1) {
                this.sourceOnlyLayouts.add(keyValue2);
            } else if (!bl && bl1) {
                this.targetOnlyLayouts.add(keyValue2);
            }
        }
    }

    public int getIndexBitOffset() {
        return this.indexBitOffset;
    }

    public boolean isLayoutValid(int[] bitShifts, List list1, Set set1) {
        boolean bl = true;
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(list1.size()));
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            LongKeyNode longKeyNode = (LongKeyNode) iterator.next();
            long mixedKey = longKeyNode.getMixedKey();
            long currentKey = longKeyNode.getCurrentKey();
            long bd = MethodParameterChanger.extractPermutedBits(currentKey, 8, 55, bitShifts);
            long be = MethodParameterChanger.extractLowPermutedBits(currentKey, 56, bitShifts);
            boolean bl1 = this.targetNodes.contains(longKeyNode);
            boolean bl2 = this.sourceNodes.contains(longKeyNode);
            if (bl2) {
                bl = bl && hashSet.add(be);
            }

            if (!bl) {
                return false;
            }

            if (bd == mixedKey) {
                return false;
            }

            if (bl1 && bd == 0L) {
                return false;
            }

            if (bl1 && bd > 140737488355327L) {
                return false;
            }

            if (bl2 && set1.contains(be)) {
                return false;
            }
        }

        return true;
    }

    public ParameterKeyGenerator(
            List list1,
            List list2,
            List list3,
            List list4,
            ObservableHolder observableHolder,
            List list5,
            List list6,
            List list7,
            List list8,
            ObservableHolder observableHolder1,
            Iterator iterator,
            Random random1,
            boolean bl
    ) throws ZkmException, IOException {
        this.random = random1;
        this.middleValueIterator = iterator;
        MethodParamChangeComparator methodParamChangeComparator = new MethodParamChangeComparator(this);
        Collections.sort(list1, methodParamChangeComparator);
        Collections.sort(list2, methodParamChangeComparator);
        list1.size();
        Integer integer = list2.size();
        this.initSizes(integer);
        this.nodesByLayout = new ListMultimap(true, this.rootPairCount / 128);
        this.layouts = new LinkedHashSet(ZkmUtils.getPrimeCapacity(this.rootPairCount / 128));
        this.generateInitialNodes(list3, list2.size(), bl);
        this.rootNode = RootKeyNode.create(this.random, this.rootPairCount, list4, this);
        PairMultiMap pairMultiMap = this.rootNode.getKeyPairMap();
        this.classifyNodes(pairMultiMap);
        this.relayoutGroups(list5);
        IdentityHashSet identityHashSet = new IdentityHashSet(this.sourceNodes.size());
        IdentityHashSet identityHashSet1 = new IdentityHashSet(this.targetNodes.size());
        IdentityMapWrapper identityMapWrapper = new IdentityMapWrapper(ZkmUtils.getPrimeCapacity(list2.size()));
        int[] layoutValue = MethodParameterChanger.createBitPermutation(this.random);
        this.addLayout(layoutValue);
        observableHolder.setValue(this.getCurrentLayout().clone());
        this.initialized = true;
        this.assignNodePairs(identityHashSet, identityHashSet1, list2, identityMapWrapper);
        this.buildKeyChains(identityHashSet, identityHashSet1, list1, list2, identityMapWrapper, list6, list7, list8);
        int[] currentLayout = this.getCurrentLayout();
        int[] valueValue = new int[currentLayout.length];
        System.arraycopy(currentLayout, 0, valueValue, 0, currentLayout.length);
        observableHolder1.setValue(valueValue);
    }

    public int getMergeSortDepthLimit() {
        return mergeSortDepthLimit;
    }

    public void linkFalseDependents(MethodParamChangeNode methodParamChangeNode, List list1) {
        List list2 = methodParamChangeNode.getFalseDependents();
        int list2Size = list2.size();
        if (list2Size != 0) {
            if (list2Size > 1) {
                list2 = new ArrayList(list2);
                ZkmUtils.shuffleList(list2, this.random);
            }

            LongKeyNode longKeyNode = methodParamChangeNode.getSecondKeyNode();
            ArrayList arrayList = new ArrayList(list2Size + 1);
            arrayList.add(new ObjectPair(methodParamChangeNode, longKeyNode));

            for (int elementIndex = 0; elementIndex < list2Size; elementIndex++) {
                MethodParamChangeNode methodParamChangeNode1 = (MethodParamChangeNode) list2.get(elementIndex);
                LongKeyNode longKeyNode1 = methodParamChangeNode1.getFirstKeyNode();
                longKeyNode.appendToChain(longKeyNode1);
                long key = this.encodeIndexKey(longKeyNode.getIndex());
                long valueValue = this.encodeIndexKey(longKeyNode1.getIndex());
                list1.add(new MutablePair(key, valueValue));
                arrayList.add(new ObjectPair(methodParamChangeNode1, longKeyNode1));
                longKeyNode = longKeyNode1;
            }
        }
    }

    public void relayoutGroups(List outputEntries) {
        sortNodes(this.allKeyNodes);
        this.pendingLayoutNodes = new IdentityHashSet(this.allKeyNodes.size());
        int nextNodeIndex = 0;
        Iterator allNodes = this.allKeyNodes.iterator();

        while (allNodes.hasNext()) {
            LongKeyNode keyNode = (LongKeyNode) allNodes.next();
            keyNode.setIndex(nextNodeIndex++);
            this.pendingLayoutNodes.add(keyNode);
        }

        HashSet usedLowKeys = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.sourceNodes.size()));
        Iterator sourceNodeIterator = this.sourceNodes.iterator();

        while (sourceNodeIterator.hasNext()) {
            LongKeyNode sourceNode = (LongKeyNode) sourceNodeIterator.next();
            usedLowKeys.add(sourceNode.getLow56Bits());
        }

        ArrayList availableLayouts = new ArrayList(this.layouts);
        ArrayList unselectedLayouts = new ArrayList(availableLayouts);
        int layoutCount = availableLayouts.size();
        ArrayList selectedLayoutNodes = new ArrayList(layoutCount * 3);
        int selectionCount = 0;

        do {
            int layoutIndex;
            if (selectionCount <= layoutCount * 2) {
                layoutIndex = this.random.nextInt(layoutCount);
            } else {
                do {
                    layoutIndex = this.random.nextInt(layoutCount);
                } while (!unselectedLayouts.contains(availableLayouts.get(layoutIndex)) || this.random.nextBoolean());
            }

            int[] layout = (int[]) availableLayouts.get(layoutIndex);
            List layoutNodes = this.nodesByLayout.getValues(layout);
            LongKeyNode selectedNode;
            do {
                int nodeIndex = this.random.nextInt(layoutNodes.size());
                selectedNode = (LongKeyNode) layoutNodes.get(nodeIndex);
            } while (selectedNode.getIndex() >= this.keyCount);

            selectedLayoutNodes.add(new ObjectPair(layout, selectedNode));
            unselectedLayouts.remove(layout);
            selectionCount++;
        } while (!unselectedLayouts.isEmpty());

        int selectionSize = selectedLayoutNodes.size();

        for (int elementIndex = 0; elementIndex < selectionSize; elementIndex++) {
            ObjectPair layoutNode = (ObjectPair) selectedLayoutNodes.get(elementIndex);
            this.relayoutGroup((int[]) layoutNode.getFirst(), (LongKeyNode) layoutNode.getSecond(), usedLowKeys, outputEntries);
        }
    }

    public void addLayout(Object layout) {
        this.bitLayouts.add(layout);
    }

    public void relayoutGroup(int[] key, LongKeyNode longKeyNode, Set set1, List list1) {
        List list2 = this.nodesByLayout.getValues(key);
        int[] sourceArray = null;
        int bc = -1;
        if (this.targetOnlyLayouts.contains(key)) {
            while (++bc <= 8192) {
                sourceArray = MethodParameterChanger.createBitPermutation(this.random);
                if (this.isLayoutValid(sourceArray, list2, set1)) {
                    break;
                }
            }
        } else {
            while (++bc <= 8192) {
                sourceArray = key.clone();
                MethodParameterChanger.reshufflePermutation(sourceArray, this.random);
                if (this.isLayoutValid(sourceArray, list2, set1)) {
                    break;
                }
            }
        }

        if (bc <= 8192) {
            int index = longKeyNode.getIndex();
            long be = this.encodeIndexKey(index);
            list1.add(be);
            list1.add(sourceArray.clone());
            HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(list2.size()));
            Iterator iterator = list2.iterator();

            while (iterator.hasNext()) {
                LongKeyNode longKeyNode1 = (LongKeyNode) iterator.next();
                if (this.sourceNodes.contains(longKeyNode1)) {
                    hashSet.add(longKeyNode1.getLow56Bits());
                }
            }

            longKeyNode.copyBitLayout(sourceArray);
            HashSet hashSet1 = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(list2.size()));
            Iterator iterator1 = list2.iterator();

            while (iterator1.hasNext()) {
                LongKeyNode longKeyNode2 = (LongKeyNode) iterator1.next();
                this.pendingLayoutNodes.remove(longKeyNode2);
                if (this.sourceNodes.contains(longKeyNode2)) {
                    hashSet1.add(longKeyNode2.getLow56Bits());
                }
            }

            iterator1 = hashSet.iterator();

            while (iterator1.hasNext()) {
                Long long1 = (Long) iterator1.next();
                set1.remove(long1);
            }

            iterator1 = hashSet1.iterator();

            while (iterator1.hasNext()) {
                Long long2 = (Long) iterator1.next();
                set1.add(long2);
            }
        }
    }

    public long generateUniqueKey() {
        int[] currentLayout = this.getCurrentLayout();
        int bb = this.random.nextInt(256);
        int bc = this.random.nextInt(256);

        long bd;
        do {
            bd = this.nextMiddleValue();
        } while (this.usedMiddleValues.contains(bd));

        this.usedMiddleValues.add(bd);
        return MethodParameterChanger.composeKey(bc, bd, bb, currentLayout);
    }

    public static void sortNodes(List nodes) {
        mergeSort(0, nodes.size() - 1, nodes, new ArrayList(nodes), 0);
    }

    public static int computeKeyCount(int ba, MutableInt mutableInt) {
        int bb = 0;
        if (HiddenOptionFlags.PARAMETER_KEY_SIZE_OVERRIDE != null) {
            try {
                bb = Integer.parseInt(HiddenOptionFlags.PARAMETER_KEY_SIZE_OVERRIDE);
            } catch (NumberFormatException numberFormatException) {
            }
        }

        int bg;
        short bh;
        if (ba <= 2010) {
            int bc = ba * 6;
            mutableInt.setValue(2);
            bg = bc;
            bh = 2048;
        } else if (ba >= 4510) {
            int be = ba * 3;
            mutableInt.setValue(5);
            bg = be;
            bh = 2048;
        } else {
            float bd = (ba - 2010) / 2500.0F;
            int bf = (int) (ba * (6.0F - 3.0F * bd));
            mutableInt.setValue(2 + Math.round(3.0F * bd));
            bg = bf;
            bh = 2048;
        }

        return Math.max(bg, Math.max(bh, bb));
    }

    public void initSizes(int ba) {
        MutableInt mutableInt = new MutableInt();
        this.keyCount = computeKeyCount(ba, mutableInt);
        int value = mutableInt.getValue();
        this.initialNodeCount = this.keyCount / value;
        this.rootPairCount = this.keyCount / 2;
        int bc = 8;
        int bf = 8;

        for (byte bg = 25; bf < bg; bg = 25) {
            if (2 << bc > this.keyCount) {
                this.indexBitOffset = 63 - bc;
                break;
            }

            bf = ++bc;
        }

        mergeSortDepthLimit = computeMergeSortDepth(this.keyCount);
        long be = (1L << this.indexBitOffset) - 1L;
        long bd = be + 1L;
        indexRandoms = this.random.longs(0L, bd).iterator();
        this.randomLongs = this.random.longs(0L, Long.MAX_VALUE).iterator();
        this.minLinkCount = this.initialNodeCount / 5;
    }

    public void assignNodePairs(Set set1, Set set2, List list1, IdentityMapWrapper identityMapWrapper) {
        Map map1 = this.rootNode.getKeyChainMap();
        int list1Size = list1.size();
        ArrayList arrayList = new ArrayList(list1Size);
        this.usedMiddleValues = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.sourceNodes.size()));
        ArrayList arrayList1 = new ArrayList(this.sourceNodes.size());
        Iterator iterator = this.sourceNodes.iterator();

        while (iterator.hasNext()) {
            LongKeyNode longKeyNode = (LongKeyNode) iterator.next();
            arrayList1.add(longKeyNode);
        }

        KeyNodeComparator keyNodeComparator = new KeyNodeComparator(this);
        Collections.sort(arrayList1, keyNodeComparator);

        do {
            ZkmAssert.assertTrue(
                    !arrayList1.isEmpty(),
                    new String[]{"Insufficient entries : " + this.sourceNodes.size() + " : " + list1Size + " : " + arrayList.size() + " : " + this.pendingLayoutNodes.size()}
            );
            int elementIndex = this.random.nextInt(arrayList1.size());
            LongKeyNode longKeyNode1 = (LongKeyNode) arrayList1.remove(elementIndex);
            if (!this.pendingLayoutNodes.contains(longKeyNode1) && !this.targetNodes.contains(longKeyNode1)) {
                EncryptionKeyChain encryptionKeyChain = (EncryptionKeyChain) map1.get(longKeyNode1);
                if (encryptionKeyChain.isKeyed()) {
                    LongKeyNode longKeyNode2 = (LongKeyNode) encryptionKeyChain;
                    if (!this.pendingLayoutNodes.contains(longKeyNode2) && !set2.contains(longKeyNode2) && !this.sourceNodes.contains(longKeyNode2)) {
                        this.usedMiddleValues.add(longKeyNode1.getMixedKey());
                        set1.add(longKeyNode1);
                        set2.add(longKeyNode2);
                        arrayList.add(new MutablePair(longKeyNode1, longKeyNode2));
                    }
                }
            }
        } while (arrayList.size() < list1Size);

        int be = 0;
        HashSet hashSet = ZkmUtils.createHashSet();
        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator1 = arrayList.iterator();

        while (iterator1.hasNext()) {
            MutablePair mutablePair = (MutablePair) iterator1.next();
            MethodParamChangeNode methodParamChangeNode = (MethodParamChangeNode) list1.get(be++);
            ProgramClass programClass1 = methodParamChangeNode.getInitMethod().getOwnerProgramClass();
            if (programClass1.hasVersionedVariants()) {
                hashSet.add(methodParamChangeNode);
            } else if (programClass1.isVersionedVariant()) {
                hashMap.put(programClass1, methodParamChangeNode);
                continue;
            }

            LongKeyNode longKeyNode3 = (LongKeyNode) mutablePair.getKey();
            LongKeyNode longKeyNode4 = (LongKeyNode) mutablePair.getValue();
            long primaryKey = this.deriveKey(longKeyNode3);
            long secondaryKey = this.generateUniqueKey();
            methodParamChangeNode.setKeys(longKeyNode3, longKeyNode4, primaryKey, secondaryKey);
            identityMapWrapper.put(longKeyNode4, methodParamChangeNode);
        }

        iterator1 = hashSet.iterator();

        while (iterator1.hasNext()) {
            MethodParamChangeNode methodParamChangeNode1 = (MethodParamChangeNode) iterator1.next();
            ProgramClass programClass2 = methodParamChangeNode1.getInitMethod().getOwnerProgramClass();
            Iterator iterator2 = programClass2.getVersionedVariants().iterator();

            while (iterator2.hasNext()) {
                ClassFileBase classFileBase = (ClassFileBase) iterator2.next();
                MethodParamChangeNode methodParamChangeNode2 = (MethodParamChangeNode) hashMap.get(classFileBase);
                methodParamChangeNode2.setKeys(
                        methodParamChangeNode1.getFirstKeyNode(),
                        methodParamChangeNode1.getSecondKeyNode(),
                        methodParamChangeNode1.getPrimaryKey(),
                        methodParamChangeNode1.getSecondaryKey()
                );
            }
        }
    }

    public long deriveKey(LongKeyNode longKeyNode) {
        int[] currentLayout = this.getCurrentLayout();
        long mixedKey = longKeyNode.getMixedKey();
        int bc = longKeyNode.hashCode();
        return MethodParameterChanger.composeKey(this.random.nextInt(256), mixedKey, bc, currentLayout);
    }

    public EncryptionKeyChain getOrCreateNode(long valueArgument, BooleanFlag booleanFlag) {
        int[] currentLayout = this.getCurrentLayout();
        int elementIndex = (int) MethodParameterChanger.extractPermutedBits(valueArgument, this.indexBitOffset, 63, currentLayout);
        if (elementIndex < this.initialNodes.size()) {
            return (EncryptionKeyChain) this.allKeyNodes.get(elementIndex);
        }

        if (this.allKeyNodes.size() % 128 == 0) {
            int[] currentLayoutValue = currentLayout.clone();
            currentLayout = currentLayoutValue;
            this.addLayout(currentLayout);
        }

        LongKeyNode longKeyNode = new LongKeyNode(valueArgument, this.getCurrentLayout(), this.nodesByLayout, this.layouts);
        longKeyNode.setIndex(this.allKeyNodes.size());
        this.allKeyNodes.add(longKeyNode);
        if (this.allKeyNodes.size() >= this.keyCount - 1) {
            booleanFlag.setValue(true);
        }

        return longKeyNode;
    }

    public static void merge(int ba, int bb, int bc, List list1, List list2) {
        int elementIndex = ba;
        int elementIndexValue = elementIndex;
        int elementIndexValue2 = bb + 1;

        for (int elementIndexValue3 = elementIndex; elementIndexValue3 <= bc; elementIndexValue3++) {
            list2.set(elementIndexValue3, list1.get(elementIndexValue3));
        }

        while (elementIndexValue <= bb && elementIndexValue2 <= bc) {
            LongKeyNode longKeyNode;
            if (((LongKeyNode) list2.get(elementIndexValue)).isOrderedBefore((EncryptionKeyChain) list2.get(elementIndexValue2))) {
                longKeyNode = (LongKeyNode) list2.get(elementIndexValue++);
            } else {
                longKeyNode = (LongKeyNode) list2.get(elementIndexValue2++);
            }

            list1.set(elementIndex, longKeyNode);
            elementIndex++;
        }

        while (elementIndexValue <= bb) {
            list1.set(elementIndex, list2.get(elementIndexValue));
            elementIndex++;
            elementIndexValue++;
        }
    }

    public static void mergeSort(int ba, int bb, List list1, List list2, int bc) {
        int bd = bc;
        if (ba < bb) {
            int be = ba + (bb - ba) / 2;
            if (++bd < mergeSortDepthLimit) {
                mergeSort(ba, be, list1, list2, bd);
                mergeSort(be + 1, bb, list1, list2, bd);
            }

            merge(ba, be, bb, list1, list2);
        }
    }

    public static int computeMergeSortDepth(int nodeCount) {
        byte bc;
        if (nodeCount > 14336) {
            if (!HiddenOptionFlags.NO_PARAMETER_KEY_SIZE_LIMIT) {
                if (nodeCount <= 16384) {
                    return 12;
                }

                if (nodeCount <= 20480) {
                    return 10;
                }

                if (nodeCount <= 24576) {
                    return 8;
                }

                if (nodeCount <= 28672) {
                    return 6;
                }

                if (nodeCount <= 32768) {
                    return 4;
                }

                byte bb;
                if (nodeCount <= 35840) {
                    bb = 2;
                } else {
                    bb = 1;
                }

                return bb;
            }

            bc = 17;
        } else {
            bc = 17;
        }

        return bc;
    }

    public void linkDependents(MethodParamChangeNode methodParamChangeNode, List list1, List list2, Set set1, Set set2) {
        List list3 = methodParamChangeNode.getDependents();
        int list3Size = list3.size();
        if (list3Size != 0) {
            if (list3Size > 1) {
                list3 = new ArrayList(list3);
                ZkmUtils.shuffleList(list3, this.random);
            }

            LongKeyNode longKeyNode = methodParamChangeNode.getSecondKeyNode();
            ArrayList arrayList = new ArrayList(list3Size + 1);
            arrayList.add(new ObjectPair(methodParamChangeNode, longKeyNode));

            for (int elementIndex = 0; elementIndex < list3Size; elementIndex++) {
                MethodParamChangeNode methodParamChangeNode1 = (MethodParamChangeNode) list3.get(elementIndex);
                LongKeyNode longKeyNode1 = methodParamChangeNode1.getFirstKeyNode();
                longKeyNode.appendToChain(longKeyNode1);
                long key = this.encodeIndexKey(longKeyNode.getIndex());
                long valueValue = this.encodeIndexKey(longKeyNode1.getIndex());
                list1.add(new MutablePair(key, valueValue));
                arrayList.add(new ObjectPair(methodParamChangeNode1, longKeyNode1));
                longKeyNode = longKeyNode1;
            }

            ObjectPair objectPair1 = (ObjectPair) arrayList.get(0);
            long randomKey = ((MethodParamChangeNode) objectPair1.getFirst()).getRandomKey();
            LongKeyNode longKeyNode4 = (LongKeyNode) objectPair1.getSecond();
            MutableLong mutableLong = new MutableLong();
            long currentKey = longKeyNode4.getCurrentKey();
            int[] bitLayout = longKeyNode4.getBitLayout();
            Random random1 = this.random;
            long xorMask = computeXorMask(randomKey, currentKey, bitLayout, mutableLong, (Set) null, random1);
            longKeyNode4.setXorMask(xorMask);
            set2.add(longKeyNode4);
            long keyValue = this.encodeIndexKey(longKeyNode4.getIndex());
            list2.add(new MutablePair(keyValue, xorMask));
            int arrayListSize = arrayList.size();

            for (int elementIndexValue = 1; elementIndexValue < arrayListSize; elementIndexValue++) {
                ObjectPair objectPair = (ObjectPair) arrayList.get(elementIndexValue);
                MethodParamChangeNode methodParamChangeNode2 = (MethodParamChangeNode) objectPair.getFirst();
                LongKeyNode longKeyNode2 = (LongKeyNode) objectPair.getSecond();
                long xorMaskValue = computeXorMask(randomKey, longKeyNode2.getCurrentKey(), longKeyNode2.getBitLayout(), mutableLong, set1, this.random);
                longKeyNode2.setXorMask(xorMaskValue);
                set2.add(longKeyNode2);
                long keyValue2 = this.encodeIndexKey(longKeyNode2.getIndex());
                longKeyNode2.addKey(mutableLong.getValue());
                list2.add(new MutablePair(keyValue2, xorMaskValue));
                long primaryKey = this.deriveKey(longKeyNode2);
                methodParamChangeNode2.replacePrimaryKey(primaryKey);
                if (!methodParamChangeNode2.hasKeyField()) {
                    LongKeyNode longKeyNode3 = methodParamChangeNode2.getSecondKeyNode();
                    long randomKeyValue = methodParamChangeNode2.getRandomKey();
                    longKeyNode3.setXorMask(randomKeyValue);
                    set2.add(longKeyNode3);
                    long keyValue3 = this.encodeIndexKey(longKeyNode3.getIndex());
                    list2.add(new MutablePair(keyValue3, randomKeyValue));
                }
            }

            Iterator iterator = list3.iterator();

            while (iterator.hasNext()) {
                MethodParamChangeNode methodParamChangeNode3 = (MethodParamChangeNode) iterator.next();
                this.linkDependents(methodParamChangeNode3, list1, list2, set1, set2);
            }
        }
    }

    public void buildKeyChains(Set set1, Set set2, List list1, List list2, IdentityMapWrapper identityMapWrapper, List list3, List list4, List list5) {
        sortNodes(this.allKeyNodes);
        int ba = 0;
        Iterator iterator = this.allKeyNodes.iterator();

        while (iterator.hasNext()) {
            LongKeyNode longKeyNode = (LongKeyNode) iterator.next();
            longKeyNode.setIndex(ba++);
        }

        IdentityHashSet identityHashSet = new IdentityHashSet(set1.size() + set2.size());
        identityHashSet.addAll(set1);
        identityHashSet.addAll(set2);
        LongStream longStream = this.random.longs(1L, 281474976710655L);
        OfLong ofLong = longStream.iterator();
        Iterator iterator1 = list2.iterator();

        while (iterator1.hasNext()) {
            MethodParamChangeNode methodParamChangeNode = (MethodParamChangeNode) iterator1.next();
            long randomKeyValue = ofLong.next();
            methodParamChangeNode.setRandomKey(randomKeyValue);
        }

        Map map1 = this.rootNode.getKeyChainMap();
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.sourceNodes.size()));
        Iterator iterator2 = this.sourceNodes.iterator();

        while (iterator2.hasNext()) {
            LongKeyNode longKeyNode1 = (LongKeyNode) iterator2.next();
            hashSet.add(longKeyNode1.getLow56Bits());
        }

        IdentityHashSet identityHashSet1 = new IdentityHashSet(this.allKeyNodes.size());
        Iterator iterator3 = list1.iterator();

        while (iterator3.hasNext()) {
            MethodParamChangeNode methodParamChangeNode1 = (MethodParamChangeNode) iterator3.next();
            this.linkDependents(methodParamChangeNode1, list3, list4, hashSet, identityHashSet1);
        }

        iterator3 = list2.iterator();

        while (iterator3.hasNext()) {
            MethodParamChangeNode methodParamChangeNode2 = (MethodParamChangeNode) iterator3.next();
            if (methodParamChangeNode2.hasFalseDependents()) {
                this.linkFalseDependents(methodParamChangeNode2, list3);
            }
        }

        MutableLong mutableLong = new MutableLong();
        Iterator iterator4 = this.allKeyNodes.iterator();

        while (iterator4.hasNext()) {
            LongKeyNode longKeyNode2 = (LongKeyNode) iterator4.next();
            if (!identityHashSet1.contains(longKeyNode2)) {
                long randomKeyValue2;
                if (identityMapWrapper.containsKey(longKeyNode2)) {
                    randomKeyValue2 = ((MethodParamChangeNode) identityMapWrapper.get(longKeyNode2)).getRandomKey();
                } else {
                    randomKeyValue2 = (Long) this.randomLongs.next();
                }

                long currentKey = longKeyNode2.getCurrentKey();
                int[] bitLayout = longKeyNode2.getBitLayout();
                Random random1 = this.random;
                long xorMask = computeXorMask(randomKeyValue2, currentKey, bitLayout, mutableLong, (Set) null, random1);
                longKeyNode2.setXorMask(xorMask);
                long key = this.encodeIndexKey(longKeyNode2.getIndex());
                list4.add(new MutablePair(key, xorMask));
                identityHashSet1.add(longKeyNode2);
            }
        }

        this.collectExtraPairs(set1, set2, identityHashSet, list5);
        int bk = Math.min(this.keyCount, this.allKeyNodes.size() - 1);
        if (!HiddenOptionFlags.SIMPLE_PARAMETER_KEYS) {
            byte bl;
            int minLinkCountValue;
            if (list3.size() < 1000) {
                bl = 4;
                minLinkCountValue = this.minLinkCount;
            } else if (list3.size() < 2000) {
                bl = 2;
                minLinkCountValue = this.minLinkCount;
            } else {
                bl = 1;
                minLinkCountValue = this.minLinkCount;
            }

            int bm = Math.max(minLinkCountValue, Math.min(map1.size() / 2, list3.size() * bl));
            int bd = 0;

            do {
                int sourceIndex = -1;
                LongKeyNode sourceNode = null;
                int sourceAttempts = 0;
                boolean attemptsExhausted = false;
                while (sourceIndex == -1) {
                    int candidateIndex = this.random.nextInt(bk + 1);
                    LongKeyNode candidate = (LongKeyNode) this.allKeyNodes.get(candidateIndex);
                    if (!identityHashSet.contains(candidate) && !candidate.hasSuccessorIn(identityHashSet)) {
                        sourceIndex = candidateIndex;
                        sourceNode = candidate;
                    }
                    if (sourceAttempts++ > bk * 10) {
                        attemptsExhausted = true;
                        break;
                    }
                }
                if (attemptsExhausted) {
                    break;
                }

                int targetIndex = -1;
                LongKeyNode targetNode = null;
                int targetAttempts = 0;
                while (targetIndex == -1) {
                    int candidateIndex = this.random.nextInt(bk + 1);
                    LongKeyNode candidate = (LongKeyNode) this.allKeyNodes.get(candidateIndex);
                    if (candidate != sourceNode && !candidate.hasPrevious() && !candidate.hasSuccessor(sourceNode)) {
                        targetIndex = candidateIndex;
                        targetNode = candidate;
                    }
                    if (targetAttempts++ > bk * 10) {
                        attemptsExhausted = true;
                        break;
                    }
                }
                if (attemptsExhausted) {
                    break;
                }

                sourceNode.appendToChain(targetNode);
                long sourceKey = this.encodeIndexKey(sourceIndex);
                long targetKey = this.encodeIndexKey(targetIndex);
                list3.add(new MutablePair(sourceKey, targetKey));
            } while (++bd < bm);
        }

        ZkmUtils.shuffleList(list3, this.random);
        ZkmUtils.shuffleList(list4, this.random);
    }

    public void collectExtraPairs(Set set1, Set set2, Set set3, List list1) {
        Map map1 = this.rootNode.getKeyChainMap();
        int ba = 0;
        Iterator iterator = map1.keySet().iterator();

        while (iterator.hasNext()) {
            LongKeyNode longKeyNode = (LongKeyNode & EncryptionKeyChain) iterator.next();
            if (!set1.contains(longKeyNode) && !set3.contains(longKeyNode) && !this.targetNodes.contains(longKeyNode)) {
                EncryptionKeyChain encryptionKeyChain = (EncryptionKeyChain) map1.get(longKeyNode);
                if (encryptionKeyChain.isKeyed()
                        && !set2.contains(encryptionKeyChain)
                        && !set3.contains(encryptionKeyChain)
                        && !this.sourceNodes.contains(encryptionKeyChain)) {
                    LongKeyNode longKeyNode1 = (LongKeyNode) encryptionKeyChain;
                    long long1Value = this.deriveKey(longKeyNode);
                    long long2Value = this.generateUniqueKey();
                    long long3 = this.generateUniqueKey();
                    list1.add(new NodePairKeys(longKeyNode, longKeyNode1, long1Value, long2Value, long3));
                    set1.add(longKeyNode);
                    set2.add(longKeyNode1);
                    set3.add(longKeyNode);
                    set3.add(longKeyNode1);
                    ba++;
                }
            }

            if (ba > 1) {
                break;
            }
        }
    }

    public static long computeXorMask(long valueArgument, long valueArgument2, int[] bitShifts, MutableLong mutableLong, Set set1, Random random1) {
        long bd = MethodParameterChanger.permuteAllBits(valueArgument2, bitShifts) & 72057594037927935L;
        long be = MethodParameterChanger.permuteBits(valueArgument, bitShifts);
        long bf = be & 72057594037927935L;
        long bg = be >>> 8 & 281474976710655L;
        long bh = be & 255L;
        OfLong ofLong = random1.longs(0L, 72057594037927936L).iterator();

        long bi;
        long valueValue;
        long bk;
        do {
            bk = ofLong.next();
            valueValue = bk << 8 | bh;
            long bl = valueValue & 72057594037927935L;
            bi = bd ^ bf ^ bl;
        } while ((bk & 281474976710655L ^ bg) == 0L || set1 != null && set1.contains(bi));

        bk = MethodParameterChanger.permuteBits(valueValue, bitShifts);
        long valueValue2 = valueArgument2 ^ valueArgument ^ bk;
        mutableLong.setValue(valueValue2);
        if (set1 != null) {
            set1.remove(bd);
            set1.add(bi);
        }

        return bk;
    }
}
