package src.client.model;

public class FileHandle {
    /* The "filehandle" is simply an integer.  We keep a counter in a
       static variable "count" so that no duplication occurs.  When
       filehandle is discarded its number becomes 0. */

    public int index;
    private int pointer = 0;
    public String url = "";
    private static int count = 1;
    public boolean readToCache = false;

    public FileHandle()
    {
	    index=++count;
    }

    public int getPointer() {
        return pointer;
    }

    public void resetPointer() {
        this.pointer = 0;
    }

    public void incrementPointer(int inc) {
        this.pointer += inc;
    }

    public int getCount() {
        return count;
    }

    public boolean isAlive()
    {
	    return (this.index!=0);
    }

    /* checks two handles are equal or not. */
    public boolean Equals(FileHandle fh) 
    { 
	    return (fh.url.equals(this.url));
    }

    /* discarding a filehandle. you do not have to use this. */
    public void discard()
    {
	    index=0;
    }
}
