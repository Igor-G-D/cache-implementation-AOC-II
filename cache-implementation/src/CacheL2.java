public class CacheL2 extends Cache {
    Cache cacheL1;

    CacheL2(int nsetsL1, int bsizeL1, int assocL1, String substL1, int nsetsL2, int bsizeL2, int assocL2, String substL2) {
        super(nsetsL2, bsizeL2, assocL2, substL2);
        cacheL1 = new Cache(nsetsL1, bsizeL1, assocL1, substL1);
    }

    public boolean accessCache(int address) {
        if (cacheL1.accessCache(address) == false) { // if there is a miss in the L1 cache
            return super.accessCache(address); // search in the L2 cache
        } // if there is a hit in the L1 cache, no need to access L2 cache
        return true;
    }

    public void printResults(int flag) {
        System.out.println("Cache L1 Results:");
        cacheL1.printResults(0);
        System.out.println("---------------------------------------------------");
        System.out.println("Cache L2 Results:");
        super.printResults(0);
    }
}
