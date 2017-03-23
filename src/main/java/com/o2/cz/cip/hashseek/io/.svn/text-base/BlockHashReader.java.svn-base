package com.o2.cz.cip.hashseek.io;

import net.sf.samtools.util.BlockCompressedInputStream;

import java.io.*;

/**
 * User: Pavel
 * Date: 24.3.13 7:15
 */
public class BlockHashReader {
    public static final int END_BIG = 0x01;
    public static final int END_SMALL = 0x02;
    public static final int END_ALL = END_BIG  | END_SMALL;
    private static final int BUFFER_SIZE = 67108864;
    public int javaHashSmall; //java hash maleho slova
    public int javaHashBig; //java hash velkeho slova
    public int smallWordLength; //delka maleho slova
    public int bigWordLength; //delka velkeho slova
    public int wordPosition = 0;
    private long filePosition = 0L;
    private FileInputStream fis;
    private int nextWordPosition = 0;
    InputStream inStream;

    int blockSize; //pokud neni 0, pouzije se blockSize, umožní řídit velikost indexu, tím že se pointer vztáhne k bloku. Pokud je 0, tak pointer ukazuje přímo na slovo.
    //je to z toho důvodu, že pro pointer použijeme místo long int, což jsou maximálně 2GB adresovatelného prostoru. Pokud např. blok roztáhneme na 8Byte, tak zvětšíme adresovatelný
    //prostor 8x na 16GB, atd.. Zároveň to má efekt, že pokud jsou v bloku duplicitní slova lze pak použít pouze distinct hodnoty a tím se velikost indexu zmenší.
    long[] customBlocks; //pokud je vyplnen, pouzije se customBlocks místo blockSize, umožní řídit velikost indexu, tím že se pointer vztáhne k předpočítaným blokům (např. bloky pro jedno beaId)
    private boolean useBlock;
    private boolean useBlockSize;
    private boolean useCustomBlocks;
    private int blockPosition =0;
    private int customBlocksIndexForwarded = 0;

    public BlockHashReader(File file, int blockSize, long[] customBlocks) throws IOException {
        fis = new FileInputStream(file);
        if(BgzUtil.isBgzFile(file)){
            inStream= new BlockCompressedInputStream(fis);
        }else{
            inStream = new BufferedInputStream(fis, BUFFER_SIZE);
        }
        this.javaHashBig = 0;
        this.javaHashSmall = 0;
        this.smallWordLength = 0;
        this.bigWordLength = 0;
        this.blockSize = blockSize;
        this.customBlocks = customBlocks;
        this.useBlock = blockSize > 0 || customBlocks != null;
        this.useBlockSize = blockSize > 0 && customBlocks == null;
        this.useCustomBlocks = customBlocks != null;
        long fileLength = BgzUtil.fileLength(file);
        if (useBlock) {
            if (useBlockSize) {
                if (fileLength/blockSize > Integer.MAX_VALUE) {
                    throw new RuntimeException(String.format("File '%s' has length '%s', block size defined '%s', calculated address space exceeds Integer.MAX_VALUE. Please specify '%s' as minimum block size.", file.getAbsolutePath(), fileLength, blockSize, (fileLength/Integer.MAX_VALUE)+1));
                }
            } else if (useCustomBlocks) {
                if (customBlocks.length > Integer.MAX_VALUE) {
                    throw new RuntimeException(String.format("File '%s' has length '%s', block size defined in custom blocks with length '%s', which is more than Integer.MAX_VALUE. Please shorten custom block size.", file.getAbsolutePath(), fileLength, customBlocks.length));
                }
            }
        } else {
            if (fileLength > Integer.MAX_VALUE) {
                throw new RuntimeException(String.format("File '%s' has length '%s',  which is more than Integer.MAX_VALUE. Block size is not defined. Please specify '%s' as minimum block size.", file.getAbsolutePath(), fileLength, (fileLength/Integer.MAX_VALUE)+1));
            }
        }
    }


    public int readWords() throws IOException {
        int end = 0;
        byte byteRead;
        javaHashSmall=0;
        smallWordLength =0;
        wordPosition = nextWordPosition;
        while (end == 0) {
            byteRead = (byte) inStream.read();
            if (byteRead == -1) {
                return 0;
            }
            filePosition++;
            if (useBlock) {
                if (useCustomBlocks) {
                    if (filePosition >= customBlocks[customBlocksIndexForwarded]) {
                        blockPosition = customBlocksIndexForwarded++; //pozice ukazuje na aktualni blok, ale index bloku je o jeden napred.
                        //tohle nepotrebujeme, na posledni blok to stejne nikdy nedojde, resp. dojde ale ten tam je jako konec souboru.
                        //if (customBlocksIndexForwarded > customBlocks.length) { // konec custom bloku
                        //    customBlocksIndexForwarded --;
                        //}
                    }
                } else { //use block size
                   blockPosition = (int) (filePosition / blockSize); //zda je mozne toto deleni a vejde se do int se kontroluje na zacatku.
                }
            } else {
                blockPosition = (int) filePosition; //zda je toto mozne (max. 2GB) a vejde se do int se kontroluje na zacatku.
            }

            if (byteRead >=0 && byteRead < 32) { //control vyfiltrujeme, utf-8 pustime dal.
                nextWordPosition = blockPosition;
                end = END_ALL;
                break;
            }
            switch (byteRead) {
                case '<':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '>':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ';':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '\"':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '=':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '/':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ' ':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '\\':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ',':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '(':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ')':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '\'':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ':':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '$':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '^':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '&':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '*':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '#':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '!':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '`':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case ']':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '[':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '?':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '+':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '%':
                    nextWordPosition = blockPosition;
                    end = END_ALL;
                    break;
                case '-':
                    javaHashBig = 31 * javaHashBig + byteRead;
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '@':
                    javaHashBig = 31 * javaHashBig + byteRead;
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '_':
                    javaHashBig = 31 * javaHashBig + byteRead;
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                case '.':
                    javaHashBig = 31 * javaHashBig + byteRead;
                    bigWordLength++;
                    end = END_SMALL;
                    break;
                default:
                    javaHashBig = 31 * javaHashBig + byteRead;
                    javaHashSmall = 31 * javaHashSmall + byteRead;
                    smallWordLength++;
                    bigWordLength++;
                    break;
            }
        }
        return end;
    }

    public void close() throws IOException {
        inStream.close();
    }


}
