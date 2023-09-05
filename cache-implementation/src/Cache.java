import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class Cache {
    private int nsets;
    private int assoc;
    private int bsize;
    private String subst;

    int indexBits;
    int offsetBits;
    int tagBits;

    private int cache[][];
    private boolean validBit[][];
    private int compulsoryMisses;
    private int capacityMisses;
    private int conflictMisses;
    private int accesses;
    private final int numberOfPositions;
    private int positionsFilled;

    private Queue<Integer>[] fifoQueue;
    private LinkedList<Integer>[] lruList;

    Cache(int nsets, int bsize, int assoc, String subst) {
        this.nsets = nsets;
        this.bsize = bsize;
        this.assoc = assoc;
        this.subst = subst;

        compulsoryMisses = 0;
        capacityMisses = 0;
        conflictMisses = 0;
        accesses = 0;

        numberOfPositions = nsets*assoc;
        positionsFilled = 0;

        this.cache = new int[nsets][assoc]; // default value = 0
        this.validBit = new boolean[nsets][assoc]; // default value = false;

        double indexDouble = Math.log(nsets) / Math.log(2); // log base 2 of nsets
        double offsetDouble = Math.log(bsize) / Math.log(2); // log base 2 of nsets

        indexBits = (int) indexDouble; // converting to integer since it's always going to be a natural number
        offsetBits = (int) offsetDouble; // converting to integer since it's always going to be a natural numbe
        tagBits = 32-indexBits-offsetBits;

        switch(subst) {
            case "r": // if substitution algorithm is random
                break; // nothing to be done
            case "f": // if substitution algorithm is FIFO
                fifoQueue = new LinkedList[nsets]; // initialize fifoQueue 
                for (int i = 0; i < nsets; i++) {
                    fifoQueue[i] = new LinkedList<>();
                }
                break;
            case "l":
                lruList = new LinkedList[nsets]; // initialize lruList 
                for (int i = 0; i < nsets; i++) {
                    lruList[i] = new LinkedList<>();
                }
                break;
            }
    }

    public boolean accessCache(int address) { // search for address in memory
        accesses++;
        int tag = address >> (offsetBits + indexBits);
        int index = (address >> offsetBits) & ((1 << indexBits) - 1); //  AND with bitmask to get the index from the address


        for(int i = 0; i < assoc;i++) {
            if(cache[index][i] == tag && validBit[index][i] == true) { // hit
                if(subst.compareTo("l") == 0) {
                    int accessedIndex = lruList[index].indexOf(tag); // search if that tag was accessed before

                    if(accessedIndex != -1) { // if yes, remove from the list
                        lruList[index].remove(accessedIndex);
                    }

                    lruList[index].addFirst(tag); // add to beggining of list (most recently used)
                }
                return true;// if there is a hit, then nothing else to do
            }
        }
        // if it reaches this part of the code, there is a miss
        treatFault(index, tag);
        return false;
    }

    private void treatFault(int index, int tag) {
        if(assoc == 1) { // directly mapped cache
            if(validBit[index][0] == false) {
                compulsoryMisses++; // if its false, means that position hasn't been accessed before
                validBit[index][0] = true; // validBit = 1 since the position was just accessed
                positionsFilled++;
            } else {
                conflictMisses++; // if not, one position needs to be replaced
            }
            cache[index][0] = tag; // bring that address to cache in that position
            return; // nothing else to be done
        } else {
            if(!(positionsFilled == numberOfPositions)) { // if the cache is full, there can't be any more compulsory misses
                for(int i = 0; i < assoc; i ++) { // check if that index has a position to be filled
                    if(validBit[index][i] == false) { 
                        compulsoryMisses++;
                        cache[index][i] = tag; // bring that address to cache in that position
                        validBit[index][i] = true; // validBit = 1 since the position was just accessed
                        positionsFilled++;
                    switch(subst) {
                        case "r": // if substitution algorithm is random
                            break; // nothing to be done
                        case "f": // if substitution algorithm is FIFO
                            fifoQueue[index].add(tag);
                            break;
                        case "l":
                            int accessedIndex = lruList[index].indexOf(tag); // search if that tag was accessed before (-1 if not)

                            if(accessedIndex != -1) { // if yes, remove from the list
                                lruList[index].remove(accessedIndex);
                            }

                            lruList[index].addFirst(tag); // add to beggining of list (most recently used)
                            break;
                        }

                        return; // nothing else to do
                    }
                }
            }

            // if code reaches here, means that it didn't find a position to be filled, needs to diferenciate between capacity and conflict faults
            
            if(positionsFilled == numberOfPositions) { // if true, means the cache is full
                capacityMisses++;
            } else {
                conflictMisses++;
            }

            switch(subst) {
                case "r":
                    randomSubst(index, tag);
                    break;
                case "f":
                    fifoSubst(index, tag);
                    break;
                case "l":
                    lruSubst(index, tag);
                    break;
            }

        }
    }

    private void randomSubst(int index, int tag) {
        int random = (int)Math.floor(Math.random() * assoc); // find a random position to replace
        cache[index][random] = tag; // replace ir
    }

    private void fifoSubst(int index, int tag) {
        int oldestTag = fifoQueue[index].remove(); // get the oldest added tag from the start of the queue

        for(int i = 0; i < assoc; i++) { // find its location
            if(cache[index][i] == oldestTag) {
                cache[index][i] = tag; // replace it
            }
        }
        fifoQueue[index].add(tag); // add new tag to the end of the queue
    }

    private void lruSubst(int index, int tag) {
        int oldestTagAccessed = lruList[index].removeLast(); // get the oldest added accessed from the end of the list

        for(int i = 0; i < assoc; i++) { // find its location
            if(cache[index][i] == oldestTagAccessed) {
                cache[index][i] = tag; // replace it
                break;
            }
        }

        lruList[index].addFirst(tag); // add tag as the most recently accessed in the lruList

    }

    public void printResults(int formatFlag) {
        double totalMissRate = (double)(compulsoryMisses + capacityMisses + conflictMisses)/accesses;
        double compulsoryMissRate = (double)compulsoryMisses/(compulsoryMisses + capacityMisses + conflictMisses);
        double capacityMissRate = (double)capacityMisses/(compulsoryMisses + capacityMisses + conflictMisses);
        double conflictMissRate = (double)conflictMisses/(compulsoryMisses + capacityMisses + conflictMisses);
        double hitRate = 1 - totalMissRate;
        if(formatFlag == 1) {
            System.out.printf(Locale.US,"%d, %.2f, %.2f, %.2f, %.2f, %.2f\n", accesses, hitRate, totalMissRate, compulsoryMissRate, capacityMissRate, conflictMissRate);
        } else {
            System.out.println("Number of sets: " + nsets);
            System.out.println("Associativity: " + assoc);
            System.out.println("Block size: " + bsize);
            System.out.println("Substitution Algorithm: " + subst);

            System.out.println("Total Accesses: " + accesses);
            System.out.printf(Locale.US,"Hit Rate: %.2f%%\n", hitRate*100 );
            System.out.printf(Locale.US,"Total Miss Rate: %.2f%%\n", totalMissRate*100 );
            System.out.printf(Locale.US,"Compulsory Miss Rate: %.2f%%\n", compulsoryMissRate*100 );
            System.out.printf(Locale.US,"Capacity Miss Rate: %.2f%%\n", capacityMissRate*100 );
            System.out.printf(Locale.US,"Conflict Miss Rate: %.2f%%\n", conflictMissRate*100 );
        }
    }
}