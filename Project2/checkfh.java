import java.io.*;

public class checkfh
{
	public static void main(String argv[])
	{
		filehandle fh1, fh2;

		fh1=new filehandle();
		fh1.url = "url1";
		System.out.printf("%d made.\n", fh1.getCount());

		if (fh1.isAlive()) 
			System.out.printf("%d is alive.\n", fh1.getCount());

		fh2=new filehandle();
		fh2.url = "url2";
		System.out.printf("%d made.\n", fh1.getCount());
		if (fh2.isAlive()) 
			System.out.printf("%d is alive.\n", fh1.getCount());

		if (fh1.Equals(fh2))
			System.out.println("one and two are same, this is strange..");
		else
			System.out.println("one and two are not equal.This is good.");
		fh1.discard();
		fh2.discard();
		if (fh1.Equals(fh2))
			System.out.println("one and two are discarded and are equal.");
		else
			System.out.println("one and two are not equal, this is strange.");
  }
}






