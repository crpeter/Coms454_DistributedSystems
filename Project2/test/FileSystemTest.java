package test;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

import src.client.system.ClientFileSystem;
import src.client.system.ClientFileSystemCache;
import src.client.system.FileSystemAPI;
import src.client.model.FileHandle;

public class FileSystemTest {

	private static String host = "127.0.0.1";
	private static int port = 10044;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {

		String filename = "testfile.txt";

		if (args.length == 2) {
			host = args[0];
			port = Integer.parseInt(args[1]);
		}

		System.out.println("IP: " + host + "; Server Port: " + port);
		Scanner scan = new Scanner(System.in);

		/* Initialise the client-side file system. The following line should be replaced by something like:
	     fileSystemAPI fs = new YourClientSideFileSystem() in your version.
		 */

		FileSystemAPI fs = new ClientFileSystem(host, port);

		System.out.print("Type y to use cache or any other key to continue: ");
		String input = scan.nextLine();
		if (input.length() > 0 && input.toLowerCase().charAt(0) == 'y') {
			fs = new ClientFileSystemCache(host, port);
		}

		String url = "127.0.0.1:" + port + "/" + filename;
		FileHandle fh = fs.open(url);

		while(true) {
			if (fh.index == 0) {
				System.out.print("Enter a url to open: ");
				String urlInput = scan.nextLine();
				fh = fs.open(urlInput);
			}
			System.out.print("Enter {0, 1, 2, 3} for {write, read, isEOF, close}: ");
			input = scan.nextLine();
			if (input.length() == 0) {
				continue;
			}
			switch (input.charAt(0)) {
				case '0':
					// Write
					// System.out.println("write to file handle: " + fh.index + "\ndata: " + dataInput);
					System.out.printf("Enter data to write at offset {%d}: ", fh.getPointer());
					input = scan.nextLine();
					input = input.replace("\\n", "\n").replace("\\t", "\t");
					fs.write(fh, input.getBytes());
					break;
				case '1':
					// Read
					// System.out.println("read from file handle: " + fh.index + ", pointer: " + fh.getPointer());
					System.out.print("Enter number of bytes to read: ");
					input = scan.nextLine();
					int n;
					try {
						n = Integer.parseInt(input);
						byte[] readData = new byte[n];
						fs.read(fh, readData);
						System.out.println("Data: \n" + new String(readData));
					} catch (Exception e) {
						System.out.println("Exception:" + e.getMessage() + " " + e);
					}
					break;
				case '2':
					// isEOF
					boolean isEnd = fs.isEOF(fh);
					System.out.println("eof: " + isEnd + ", current pointer: " + fh.getPointer());
					break;
				case '3':
					// Close
					fs.close(fh);
					System.out.println("fh closed");
					break;
			}
		}
		// int res;
		// byte[] data = new byte[100];
		// filehandle fh = fs.open(url);
		
		// System.out.println("Start reading file...");
		// while (!fs.isEOF(fh)) {
		// 	// read data.
		// 	res = fs.read(fh, data);
		// 	// System.out.println("data: " + new String(data));
		// }
		// System.out.println("Done reading file...");	
		
		
		// System.out.println("Please enter data to write to file or 'q' to stop");
		// String contents = scan.nextLine();
		
		// while (!(contents.equals("q"))) {
		// 		contents = contents + "\n";
		// 		byte[] toWrite = contents.getBytes();				
		// 		boolean write_res = fs.write(fh, toWrite);
		// 		System.out.println("Data is written to file");
		// 		System.out.println("Please enter data to write to file or 'q' to stop");
		// 		contents = scan.nextLine();
		// }
	
		// fs.close(fh);
		// return;
	}
}