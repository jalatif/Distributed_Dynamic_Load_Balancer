package DLB.Utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Created by saikat on 4/26/15.
 */
public class CompressedBlockOutputStream extends FilterOutputStream {
    /**
     * Buffer for input data
     */
    private byte[] inBuf = null;

    /**
     * Buffer for compressed data to be written
     */
    private byte[] outBuf = null;

    /**
     * Number of bytes in the buffer
     */
    private int len = 0;

    /**
     * Deflater for compressing data
     */
    private Deflater deflater = null;

    /**
     * Constructs a CompressedBlockOutputStream that writes to
     * the given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The default
     * compression strategy and level are used.
     */
    public CompressedBlockOutputStream(OutputStream os, int size)
            throws IOException {
        this(os, size,
                Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    /**
     * Constructs a CompressedBlockOutputStream that writes to the
     * given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The compression
     * level and strategy should be specified using the constants
     * defined in java.util.zip.Deflator.
     */
    public CompressedBlockOutputStream(OutputStream os, int size,
                                       int level, int strategy) throws IOException {
        super(os);
        this.inBuf = new byte[size];
        this.outBuf = new byte[size + 64];
        this.deflater = new Deflater(level);
        this.deflater.setStrategy(strategy);
    }

    protected void compressAndSend() throws IOException {
        if (len > 0) {
            deflater.setInput(inBuf, 0, len);
            deflater.finish();
            int size = deflater.deflate(outBuf);

            // Write the size of the compressed data, followed
            // by the size of the uncompressed data
            out.write((size >> 24) & 0xFF);
            out.write((size >> 16) & 0xFF);
            out.write((size >>  8) & 0xFF);
            out.write((size >>  0) & 0xFF);

            out.write((len >> 24) & 0xFF);
            out.write((len >> 16) & 0xFF);
            out.write((len >>  8) & 0xFF);
            out.write((len >>  0) & 0xFF);

            out.write(outBuf, 0, size);
            out.flush();

            len = 0;
            deflater.reset();
        }
    }

    public void write(int b) throws IOException {
        inBuf[len++] = (byte) b;
        if (len == inBuf.length) {
            compressAndSend();
        }
    }

    public void write(byte[] b, int boff, int blen)
            throws IOException {
        while ((len + blen) > inBuf.length) {
            int toCopy = inBuf.length - len;
            System.arraycopy(b, boff, inBuf, len, toCopy);
            len += toCopy;
            compressAndSend();
            boff += toCopy;
            blen -= toCopy;
        }
        System.arraycopy(b, boff, inBuf, len, blen);
        len += blen;
    }

    public void flush() throws IOException {
        compressAndSend();
        out.flush();
    }

    public void close() throws IOException {
        compressAndSend();
        out.close();
    }
}

