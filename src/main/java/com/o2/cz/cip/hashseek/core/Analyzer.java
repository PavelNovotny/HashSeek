package com.o2.cz.cip.hashseek.core;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by pavelnovotny on 24.04.17.
 */
public class Analyzer {

    public static final int END_BIG = 0x01;
    public static final int END_SMALL = 0x02;
    public static final int END_ALL = END_BIG  | END_SMALL;
    public static int MAX_WORD_LEN = 300;
    public static int MAX_WORD_COUNT = 100000;
    public int bytePosition = 0;
    public byte[] document;
    private byte[] smallWord;
    private byte[] bigWord;
    public int smallWordLength; //delka maleho slova
    public int bigWordLength; //delka velkeho slova
    private byte[][] words;
    int wordCount;

    public Analyzer(byte[] document) throws IOException {
        this.smallWord = new byte[MAX_WORD_LEN];
        this.bigWord = new byte[MAX_WORD_LEN];
        this.words = new byte[MAX_WORD_COUNT][];
        this.document = document;
        this.smallWordLength = 0;
        this.bigWordLength = 0;
        this.bytePosition = 0;
        this.wordCount = 0;
    }

    public byte[][] analyze() {
        int end;
        while ((end = readWord()) > 0) {
            switch (end) {
                case END_ALL:
                    copyWord(smallWordLength, smallWord);
                    if (smallWordLength != bigWordLength) {
                        copyWord(bigWordLength, bigWord);
                    }
                    bigWordLength=0;
                    break;
                case END_SMALL:
                    copyWord(smallWordLength, smallWord);
                    break;
                default:
                    break;
            }
        }
        return Arrays.copyOf(words, wordCount);
    }

    private void copyWord(int len, byte[] word) {
        if (len > 0) {
            words[wordCount++] = Arrays.copyOf(word, len);
        }
    }

    private byte read() {
        return document[bytePosition++];
    }


    public int readWord() {
        int end = 0;
        byte byteRead;
        smallWordLength =0;
        while (end == 0) {
            byteRead = read();
            if (bytePosition+1 > document.length) {
                return 0;
            }
            if (byteRead >=0 && byteRead < 32) { //control vyfiltrujeme, utf-8 pustime dal.
                end = END_ALL;
                break;
            }
            switch (byteRead) {
                case '<':
                    end = END_ALL;
                    break;
                case '>':
                    end = END_ALL;
                    break;
                case ';':
                    end = END_ALL;
                    break;
                case '\"':
                    end = END_ALL;
                    break;
                case '=':
                    end = END_ALL;
                    break;
                case '/':
                    end = END_ALL;
                    break;
                case ' ':
                    end = END_ALL;
                    break;
                case '\\':
                    end = END_ALL;
                    break;
                case ',':
                    end = END_ALL;
                    break;
                case '(':
                    end = END_ALL;
                    break;
                case ')':
                    end = END_ALL;
                    break;
                case '\'':
                    end = END_ALL;
                    break;
                case ':':
                    end = END_ALL;
                    break;
                case '$':
                    end = END_ALL;
                    break;
                case '^':
                    end = END_ALL;
                    break;
                case '&':
                    end = END_ALL;
                    break;
                case '*':
                    end = END_ALL;
                    break;
                case '#':
                    end = END_ALL;
                    break;
                case '!':
                    end = END_ALL;
                    break;
                case '`':
                    end = END_ALL;
                    break;
                case ']':
                    end = END_ALL;
                    break;
                case '[':
                    end = END_ALL;
                    break;
                case '?':
                    end = END_ALL;
                    break;
                case '+':
                    end = END_ALL;
                    break;
                case '%':
                    end = END_ALL;
                    break;
                case '-':
                    bigWord[bigWordLength++]=byteRead;
                    end = END_SMALL;
                    break;
                case '@':
                    bigWord[bigWordLength++]=byteRead;
                    end = END_SMALL;
                    break;
                case '_':
                    bigWord[bigWordLength++]=byteRead;
                    end = END_SMALL;
                    break;
                case '.':
                    bigWord[bigWordLength++]=byteRead;
                    end = END_SMALL;
                    break;
                default:
                    bigWord[bigWordLength++]=byteRead;
                    smallWord[smallWordLength++]=byteRead;
                    break;
            }
        }
        return end;
    }

}
