package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.logs.AbstractLogTransformer;
import com.o2.cz.cip.hashseek.logs.auditlog.AuditLogTransformer;
import com.o2.cz.cip.hashseek.logs.bpmlog.BpmLogTransformer;
import com.o2.cz.cip.hashseek.logs.noelog.NoeLogTransformer;
import com.o2.cz.cip.hashseek.logs.timelog.TimeLogTransformer;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pavelnovotny on 29.03.17.
 */
public class TransformAndIndex {

    List<AbstractLogTransformer> transformers=new ArrayList<AbstractLogTransformer>();
    private static Logger LOGGER = Logger.getLogger(TransformAndIndex.class);
    private BlockHashFileCreator blockHashFileCreator;

    void registerTransformers() {
        transformers.add(new BpmLogTransformer());
        transformers.add(new AuditLogTransformer());
        transformers.add(new TimeLogTransformer());
        transformers.add(new NoeLogTransformer());
    }

    public static void main (String args[]) throws Exception {
        TransformAndIndex transformAndIndex =  new TransformAndIndex();
        transformAndIndex.blockHashFileCreator = new BlockHashFileCreator();
        transformAndIndex.createHashFile(new File(args[0]));
    }

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
        blockHashFileCreator.createHashFile(file);
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
