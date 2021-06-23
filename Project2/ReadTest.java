/* standard java classes. */
import java.io.*;
import java.net.Socket;
import java.util.*;

/* fileSystemAPI should be implemented by your client-side file system. */

public class ReadTest {

	private final static String host = "127.0.0.1";
	private final static int port = 10044;

	public static void main(String[] args) throws java.lang.InterruptedException, java.io.IOException {
		String filename = "largedata.txt";
		String url = host + ":" + port + "/" + filename;
		 		 
		System.out.println("IP: " + host + "; Default Server PortNum: " + port);				
		/* Initialise the client-side file system. The following line should be replaced by something like:
	     fileSystemAPI fs = new YourClientSideFileSystem() in your version.
		 */
		 
		ClientFileSystem fs = new ClientFileSystem(host, port);
		test(fs, url);

		ClientFileSystemCache fsc = new ClientFileSystemCache(host, port);
		test(fsc, url);
	}

	public static void test(fileSystemAPI fs, String url) {
		filehandle fh = fs.open(url);
		
		long startTime, endTime;
		long turnAround;
		
		int count = 1;
		int totalTime = 0;
		byte[] data = new byte[1024];

		// repeat reading remote data and displaying turnaround time.
		while (true) {
			if (count == 20) {
				break;
			}
			// open file.
			fh = fs.open(url);
			// read the whole file, check the time needed.
			startTime = Calendar.getInstance().getTime().getTime();
			try {
				while (!fs.isEOF(fh)) {
					// read data.
					fs.read(fh, data);
				}
			} catch (Exception e) {

			}
			endTime = Calendar.getInstance().getTime().getTime();
			turnAround = endTime - startTime;
			
			// print the turnaround time.
			System.out.print(".");
			// System.out.println("Round "+ count+", This round took " + turnAround + " ms.");
			totalTime += turnAround;

			try {
				// wait a bit.
				Thread.sleep(65);
			} catch (Exception e) {

			}

			count++;
		}
		
		System.out.printf("Total %d rounds, Average turnaround time: %d ms\n", count, totalTime/20);
	}

}
