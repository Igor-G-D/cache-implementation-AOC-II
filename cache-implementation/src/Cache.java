import java.util.LinkedList;
import java.util.Queue;

public class Cache {
    private int nsets;
    private int assoc;
    private String subst;

    int indexBits;
    int offsetBits;
    int tagBits;

    private int cache[][];
    private int compulsoryMisses;
    private int capacityMisses;
    private int conflictMisses;
    private int accesses;

    private Queue<Integer>[] fifoQueue;
    private LinkedList<Integer>[] lruList;

    private boolean full;

    Cache(int nsets, int bsize, int assoc, String subst) {
        this.nsets = nsets;
        this.assoc = assoc;
        this.subst = subst;

        compulsoryMisses = 0;
        capacityMisses = 0;
        conflictMisses = 0;
        accesses = 0;

        this.cache = new int[nsets][assoc];

        double indexDouble = Math.log(nsets) / Math.log(2); // log base 2 of nsets
        double offsetDouble = Math.log(bsize) / Math.log(2); // log base 2 of nsets

        indexBits = (int) indexDouble; // converting to integer since it's always going to be a natural number
        offsetBits = (int) offsetDouble; // converting to integer since it's always going to be a natural numbe
        tagBits = 32-indexBits-offsetBits;

        for(int i = 0; i < nsets; i ++) {
            for(int j = 0; j < assoc; j ++) {
                cache[i][j] = -1; // initialize all positions to -1 to handle compulsory misses later
            }
        }

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

    public void accessCache(int address) { // search for address in memory
        accesses++;
        int tag = address >> (offsetBits + indexBits);
        int index = (address >> offsetBits) & ((1 << indexBits) - 1); //  AND with bitmask to get the index from the address


        for(int i = 0; i < assoc;i++) {
            if(cache[index][i] == tag) { // hit
                if(subst.compareTo("l") == 0) {
                    int accessedIndex = lruList[index].indexOf(tag); // search if that tag was accessed before (-1 if not)

                    if(accessedIndex != -1) { // if yes, remove from the list
                        lruList[index].remove(accessedIndex);
                    }

                    lruList[index].addFirst(tag); // add to beggining of list (most recently used)
                }
                return;// if there is a hit, then nothing else to do
            }
        }
        // if it reaches this part of the code, there is a miss
        treatFault(index, tag);
    }

    private void treatFault(int index, int tag) {
        if(assoc == 1) { // directly mapped cache
            if(cache[index][0] == -1) {
                compulsoryMisses++; // if its -1, means that position hasn't been accessed before
            } else {
                conflictMisses++; // if not, one position needs to be replaced
            }
            cache[index][0] = tag; // bring that address to cache in that position
            return; // nothing else to be done
        } else {
            if(!full) { // if the cache is full, there can't be any more compulsory misses
                for(int i = 0; i < assoc; i ++) { // check if that index has a position to be filled
                    if(cache[index][i] == -1) { 
                        compulsoryMisses++;
                        cache[index][i] = tag; // bring that address to cache in that position

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
            
            if(full) { // if already full, no need to redo the testing, it's already a capacity fault
                capacityMisses++;
            } else {
                full = testFull(); //redo the test since it may have changed
                if(full) {
                    capacityMisses++;
                } else {
                    conflictMisses++;
                }
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

    private boolean testFull() { // test if the cache is full
        for(int i = 0; i < nsets; i ++) {
            for(int j = 0; j < assoc; j ++) {
                if(cache[i][j] == -1) {
                    return false;
                }
            }
        }
        return true;
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
        double compulsoryMissRate = (double)compulsoryMisses/accesses;
        double capacityMissRate = (double)capacityMisses/accesses;
        double conflictMissRate = (double)conflictMisses/accesses;
        if(formatFlag == 1) {
            System.out.printf("%d, %.2f, %.2f, %.2f, %.2f, %.2f\n", accesses, totalMissRate, compulsoryMissRate, capacityMissRate, conflictMissRate);
        } else {
            System.out.println("Total Accesses: " + accesses);
            System.out.printf("Total Miss Rate: %.2f%%\n", totalMissRate*100 );
            System.out.printf("Compulsory Miss Rate: %.2f%%\n", compulsoryMissRate*100 );
            System.out.printf("Capacity Miss Rate: %.2f%%\n", capacityMissRate*100 );
            System.out.printf("Conflict Miss Rate: %.2f%%\n", conflictMissRate*100 );
        }
    }
}