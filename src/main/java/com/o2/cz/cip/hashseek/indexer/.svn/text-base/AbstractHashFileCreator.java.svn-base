package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.logs.AbstractLogTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tato trida zobecnuje ruzne zpusoby hashovani:
 *  - stary zpusob 50vyskytu
 *  - blokove hashovani
 *  - ...
 * Created by mfrydl on 11.3.14.
 */
public abstract class AbstractHashFileCreator {
    static final Logger LOGGER= LoggerFactory.getLogger(AbstractHashFileCreator.class);
    List<AbstractLogTransformer> transformers=new ArrayList<AbstractLogTransformer>();

    abstract void registerTransformers();

    abstract void createHashFileInner(File file) throws Exception;

    public void createHashFile(File file) throws Exception{
        registerTransformers();
        for (int i = 0; i < transformers.size(); i++) {
            AbstractLogTransformer abstractLogTransformer =  transformers.get(i);
            if(abstractLogTransformer.canBeProcessed(file)){
                long startTime=System.currentTimeMillis();
                LOGGER.info("Start - "+abstractLogTransformer.getClass().getName()+".processFileBeforeComputeHash");
                abstractLogTransformer.processFileBeforeComputeHash(file);
                long runTime=(System.currentTimeMillis()-startTime)/1000;
                LOGGER.info("End - "+abstractLogTransformer.getClass().getName()+".processFileBeforeComputeHash. Time="+runTime);
            }
        }
        long startTime=System.currentTimeMillis();
        LOGGER.info("Start - "+"createHashFile");
        createHashFileInner(file);
        long runTime=(System.currentTimeMillis()-startTime)/1000;
        LOGGER.info("End - "+"createHashFile. Time="+runTime);

        for (int i = 0; i < transformers.size(); i++) {
            AbstractLogTransformer abstractLogTransformer =  transformers.get(i);
            if(abstractLogTransformer.canBeProcessed(file)){
                startTime=System.currentTimeMillis();
                LOGGER.info("Start - "+abstractLogTransformer.getClass().getName()+".processFileAfterComputeHash");
                abstractLogTransformer.processFileAfterComputeHash(file);
                runTime=(System.currentTimeMillis()-startTime)/1000;
                LOGGER.info("End - "+abstractLogTransformer.getClass().getName()+".processFileAfterComputeHash. Time="+runTime);
            }
        }
    }


}
