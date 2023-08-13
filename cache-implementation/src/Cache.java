public class Cache {
    private int nsets;
    private int bsize;
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

    private boolean full;

    Cache(int nsets, int bsize, int assoc, String subst) {
        this.nsets = nsets;
        this.bsize = bsize;
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
    }

    public void accessCache(int address) { // search for address in memory
        accesses++;
        int tag = address >> (offsetBits + indexBits);
        int index = (address >> offsetBits) & ((1 << indexBits) - 1); //  AND with bitmask to get the index from the address


        for(int i = 0; i < assoc;i++) {
            if(cache[index][i] == tag) { // hit
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
                conflictMisses++; // if not, it needs to be replaced
            }
            cache[index][0] = tag; // bring that address to cache in that position
            return; // nothing else to be done
        } else {
            if(!full) { // if the cache is full, there can't be any more compulsory misses
                for(int i = 0; i < assoc; i ++) { // check if that index has a position to be filled
                    if(cache[index][i] == -1) { 
                        compulsoryMisses++;
                        cache[index][i] = tag; // bring that address to cache in that position
                        return; // nothing else to do
                    }
                }
            }

            // if code reaches here, means that it didn't find a position to be filled, needs to diferenciate between capacity and conflict
            
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
                    // TODO: FIFO substitution algorithm
                    break;
                case "l":
                    // TODO: LRU substitution algorithm
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
        int random = (int)Math.floor(Math.random() * assoc);
        cache[index][random] = tag;
    } 
}
