package com.o2.cz.cip.hashseek.core;

import com.o2.cz.cip.hashseek.app.AppProperties;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mfrydl on 17.3.2014
 */
public class HashFileCreatorFactory {
    private static Logger LOGGER = Logger.getLogger(HashFileCreatorFactory.class);

    Map<String,AbstractHashFileCreator> hashFileCreatorMap=new HashMap<String, AbstractHashFileCreator>();

    static HashFileCreatorFactory instance;

    public static HashFileCreatorFactory instance(){
        if(instance==null){
           instance=new HashFileCreatorFactory();
           instance.registerHashFileCreator(HashSeekConstants.OLD_INDEXER_TYPE,new HashFileCreator());
           instance.registerHashFileCreator(HashSeekConstants.BLOCK_INDEXER_TYPE,new BlockHashFileCreator());
        }
        return instance;
    }

    private void registerHashFileCreator(String id,AbstractHashFileCreator abstractHashFileCreator){
        hashFileCreatorMap.put(id,abstractHashFileCreator);
    }

    public AbstractHashFileCreator getHashFileCreator(String id){
        return hashFileCreatorMap.get(id);
    }

    public static void main (String args[]) throws Exception {
        String indexerType=AppProperties.getIndexerType();
        HashFileCreatorFactory hashFileCreatorFactory=instance();
        AbstractHashFileCreator abstractHashFileCreator=hashFileCreatorFactory.getHashFileCreator(indexerType);
        LOGGER.info("Nalezen "+abstractHashFileCreator+ " pro indexerType="+indexerType);
        if(abstractHashFileCreator==null){
            LOGGER.error("Nenalezen "+abstractHashFileCreator+ " pro indexerType="+indexerType);
            throw new NullPointerException("Nenalezen "+abstractHashFileCreator+ " pro indexerType="+indexerType);
        }else{
            abstractHashFileCreator.createHashFile(new File(args[0]));
        }

    }
}
