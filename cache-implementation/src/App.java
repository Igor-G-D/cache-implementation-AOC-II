import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception { // <nsets> <bsize> <assoc> <subst> <outputflag> <file> 

        Cache cache = new Cache(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);

        String currentDir = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDir);

        String relativePath = "./cache-implementation/addresses/" + args[5]; // Relative path to the binary file
        try (FileInputStream fileInputStream = new FileInputStream(relativePath); // if file not found
            DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            byte[] buffer = new byte[4]; // 4 bytes = 32 bits

            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                if (bytesRead == 4) {
                    int address = ((buffer[0] & 0xFF) << 24) | // and operation with the most significant byte, shifts all the way to the left
                                ((buffer[1] & 0xFF) << 16) | // and operation with the 2nd most significant byte, shifts to the proper position
                                ((buffer[2] & 0xFF) << 8) | // and operation with the 3rd most significant byte, shifts to the proper position
                                (buffer[3] & 0xFF); // or operation with all 4 to convert into a 32bit number (int)
                    
                    System.out.println("Read value: " + address);
                    cache.accessCache(address);
                }
            }
        } catch (IOException e) {
            System.out.println("File '"+ relativePath +"' not found!");
            e.printStackTrace();
        }
        System.out.println("test");
    }
}
