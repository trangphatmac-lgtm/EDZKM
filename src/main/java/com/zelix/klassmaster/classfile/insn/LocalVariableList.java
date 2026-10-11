package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.MethodSignature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class LocalVariableList implements LocalVariableProvider {
    public int[] shuffledIndices;
    public MethodBytecode methodBytecode;
    private List slots;

    public int getLastSlotIndex() {
        return this.slots.isEmpty() ? -1 : ((LocalVariableSlot) this.slots.get(this.slots.size() - 1)).getIndex();
    }

    public void clearWideFlagsFrom(int ba) {
        for (int elementIndex = ba; elementIndex < this.slots.size(); elementIndex++) {
            LocalVariableSlot localVariableSlot1 = (LocalVariableSlot) this.slots.get(elementIndex);
            localVariableSlot1.clearWideFirstHalf();
            localVariableSlot1.clearWideSecondHalf();
        }
    }

    public LocalVariableSlot insertSlot(int index, boolean bl, int insertionKind) {
        LocalVariableSlot localVariableSlot1;
        if (bl) {
            LocalVariableSlot localVariableSlot2 = new LocalVariableSlot(index + 1, false, true, insertionKind);
            this.slots.add(index, localVariableSlot2);
            localVariableSlot1 = new LocalVariableSlot(index, true, false, insertionKind);
            this.slots.add(index, localVariableSlot1);
        } else {
            localVariableSlot1 = new LocalVariableSlot(index, insertionKind);
            this.slots.add(index, localVariableSlot1);
        }

        return localVariableSlot1;
    }

    public LocalVariableSlot getPenultimateSlot() {
        return this.slots.isEmpty() ? null : (LocalVariableSlot) this.slots.get(this.slots.size() - 2);
    }

    public int renumberSlots() {
        int index = 0;

        for (int elementIndex = 0; elementIndex < this.slots.size(); elementIndex++) {
            ((LocalVariableSlot) this.slots.get(elementIndex)).replaceIndex(index);
            index++;
        }

        return ((LocalVariableSlot) this.slots.get(this.slots.size() - 1)).getIndex();
    }

    public int[] getShuffledIndices() {
        return this.shuffledIndices;
    }

    public boolean shuffleSlots(int ba, int bb, Random random1) {
        ArrayList arrayList = new ArrayList(this.slots.size());

        for (int elementIndex = ba; elementIndex < bb; elementIndex++) {
            ArrayList arrayList1 = new ArrayList();
            arrayList.add(arrayList1);
            LocalVariableSlot localVariableSlot1 = (LocalVariableSlot) this.slots.get(elementIndex);
            arrayList1.add(localVariableSlot1);
            int elementIndexValue = elementIndex;

            while (localVariableSlot1.isWideFirstHalf() || localVariableSlot1.isWideSecondHalf() && elementIndexValue < bb - 1) {
                localVariableSlot1 = (LocalVariableSlot) this.slots.get(++elementIndexValue);
                if (!localVariableSlot1.isWideFirstHalf() && !localVariableSlot1.isWideSecondHalf()) {
                    break;
                }

                arrayList1.add(localVariableSlot1);
            }

            elementIndex += arrayList1.size() - 1;
        }

        boolean bl = false;
        if (arrayList.size() > 1) {
            Collections.shuffle(arrayList, random1);
            this.shuffledIndices = new int[bb - ba];
            int be = ba;
            Iterator iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                List list1 = (List) iterator1.next();
                Iterator iterator = list1.iterator();

                while (iterator.hasNext()) {
                    LocalVariableSlot localVariableSlot2 = (LocalVariableSlot) iterator.next();
                    this.shuffledIndices[be - ba] = localVariableSlot2.getIndex();
                    this.slots.set(be++, localVariableSlot2);
                }
            }

            this.renumberSlots();
            bl = true;
        }

        return bl;
    }

    public LocalVariableList(MethodBytecode methodBytecode1, int ba) {
        this.methodBytecode = methodBytecode1;
        this.slots = new ArrayList(ba);
        String descriptor = this.methodBytecode.getMethod().getDescriptor();
        this.addParameterSlots(this.methodBytecode.isStatic(), descriptor);
    }

    public static boolean isWideType(String string) {
        return string.equals("J") || string.equals("D");
    }

    public int getSlotCount() {
        return this.slots.size();
    }

    public void setMethodBytecode(MethodBytecode methodBytecode1) {
        this.methodBytecode = methodBytecode1;
    }

    @Override
    public LocalVariableSlot lookupSlot(int ba, LocalVariableAccessKind localVariableAccessKind, int insertionKind) {
        LocalVariableSlot localVariableSlot1;
        if (this.slots.size() > ba) {
            localVariableSlot1 = (LocalVariableSlot) this.slots.get(ba);
            if (localVariableAccessKind.isWide()) {
                localVariableSlot1.clearWideSecondHalf();
                localVariableSlot1.markWideFirstHalf();
                if (this.slots.size() > ba + 1) {
                    LocalVariableSlot localVariableSlot2 = (LocalVariableSlot) this.slots.get(ba + 1);
                    localVariableSlot2.clearWideFirstHalf();
                    localVariableSlot2.markWideSecondHalf();
                } else {
                    LocalVariableSlot localVariableSlot4 = new LocalVariableSlot(ba + 1, false, true, insertionKind);
                    this.slots.add(localVariableSlot4);
                }
            } else {
                if (localVariableSlot1.isWideFirstHalf()) {
                    LocalVariableSlot localVariableSlot5 = (LocalVariableSlot) this.slots.get(ba + 1);
                    localVariableSlot5.clearWideSecondHalf();
                    localVariableSlot5.clearWideFirstHalf();
                }

                localVariableSlot1.clearWideFirstHalf();
                localVariableSlot1.clearWideSecondHalf();
            }
        } else {
            for (int slotsSize = this.slots.size(); slotsSize < ba; slotsSize++) {
                LocalVariableSlot localVariableSlot3 = new LocalVariableSlot(slotsSize, insertionKind);
                this.slots.add(localVariableSlot3);
            }

            if (localVariableAccessKind.isWide()) {
                localVariableSlot1 = new LocalVariableSlot(ba, true, false, insertionKind);
                this.slots.add(localVariableSlot1);
                LocalVariableSlot localVariableSlot6 = new LocalVariableSlot(ba + 1, false, true, insertionKind);
                this.slots.add(localVariableSlot6);
            } else {
                localVariableSlot1 = new LocalVariableSlot(ba, insertionKind);
                this.slots.add(localVariableSlot1);
            }
        }

        return localVariableSlot1;
    }

    public LocalVariableSlot getSlotAt(int elementIndex) {
        return !this.slots.isEmpty() && elementIndex <= this.slots.size() - 1 ? (LocalVariableSlot) this.slots.get(elementIndex) : null;
    }

    public void moveTrailingSlots(int ba, Integer integer) {
        int slotsSize = this.slots.size();
        int elementIndex = slotsSize - ba;
        if (elementIndex != 0) {
            ArrayList arrayList = new ArrayList(ba);

            for (int elementIndexValue = elementIndex; elementIndexValue < slotsSize; elementIndexValue++) {
                LocalVariableSlot localVariableSlot1 = (LocalVariableSlot) this.slots.remove(elementIndex);
                arrayList.add(localVariableSlot1);
            }

            this.slots.addAll(integer, arrayList);
            this.renumberSlots();
        }
    }

    public LocalVariableList(boolean bl, String descriptor, int ba) {
        this.slots = new ArrayList(ba);
        this.addParameterSlots(bl, descriptor);
    }

    public LocalVariableSlot removeSlot(int elementIndex) {
        LocalVariableSlot localVariableSlot1 = (LocalVariableSlot) this.slots.remove(elementIndex);
        if (localVariableSlot1.isWideFirstHalf()) {
            LocalVariableSlot localVariableSlot2 = (LocalVariableSlot) this.slots.remove(elementIndex);
        }

        return localVariableSlot1;
    }

    public void addParameterSlots(boolean bl, String descriptor) {
        List list1 = MethodSignature.splitParameterDescriptors(descriptor);
        int ba = 0;
        if (!bl) {
            ba++;
            this.slots.add(new LocalVariableSlot(0));
        }

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            if (isWideType((String) list1.get(elementIndex))) {
                int index = ba++;
                this.slots.add(new LocalVariableSlot(index, true, false));
                int indexValue = ba++;
                this.slots.add(new LocalVariableSlot(indexValue, false, true));
            } else {
                int indexValue2 = ba++;
                this.slots.add(new LocalVariableSlot(indexValue2));
            }
        }
    }
}
