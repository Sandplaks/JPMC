package com.jpmc.wm.lm.margin.letters.restservice.controller;



import com.jpmc.wm.lm.margin.letters.restservice.beans.BatchRequest;
import com.jpmc.wm.lm.margin.letters.restservice.beans.BatchResponse;
import com.jpmc.wm.lm.margin.letters.restservice.services.MargineLetterServices;
import com.jpmc.wm.lm.margin.letters.restservice.utils.MargineLetterUtils;
import com.jpmc.wm.lm.margin.letters.restservice.utils.SNSUtil;

import innowake.mee.jcl.api.JCLMain;
import jakarta.servlet.http.HttpServletResponse;
import innowake.lib.core.log.Logger;
import innowake.lib.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.jpmc.wm.lm.margin.letters.restservice.constant.MargineLetterConstant.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

/**
 * REST Controller for handling API requests
 */
@RestController
public class MargineLettersController {
	 Logger LOGGER = LoggerFactory.getLogger(MargineLettersController.class);
    private static final String TEST_BATCH = "/getBatchDetails";
    private static final String CALL_BATCH = "/callBatchDetails";
    private static final String S3_EFS = "/writeS3toEFS";
    private static final String EFS_FILES = "/efsFiles";
    private static final String MOVECONTENT = "/moveContent";
    private static final String DEPENDENCY_CHECK ="/jobDependencyCheck";
    private static final String S3_TO_EFS ="/s3toefsTransfer";
    private static final String TRANS_FILES_BATCH_ROOT = "/transferFilestoBatchRoot";
    
       
    @Autowired
    private MargineLetterServices services;
    
    @Autowired
     private MargineLetterUtils utils;
    @Autowired
    private SNSUtil snsUtil;
    
    @Value("${spring.profiles.active}")
    private String activeProfiles;

//    @Scheduled(initialDelayString = "5000", fixedDelayString = "60000")
//    public void watchFiles() {
//        LocalDateTime currentDateTime = LocalDateTime.now();
//        LOGGER.info("#New Application is Running {}", currentDateTime);
//    }

    @PostMapping(MOVECONTENT)
    public String moveContent(@RequestBody JSONObject request){
    	String msg="";
    	LOGGER.info("Start moveContent() Method");
    	try {
    		    String jobId = (String)request.get("jobId");		
	            
	            boolean flag =services.contentFileMovetoS3(jobId);
	            if(flag)
	            {	  
	        	   LOGGER.info(jobId+": Content file moved to S3 successfully");
	        	   msg="Content Moved to S3 Successfully";
    			
	            }
    	}catch(Exception ex) 
    	{
    		LOGGER.error(FAILURE+" During Content file move to S3 xptr: " , ex);
    		msg="Error: "+ex;
    	}
    	LOGGER.info("End moveContent() Method");
    	
    	return msg;
    }
    
    @PostMapping(EFS_FILES)
    public JSONArray efsFilesList(@RequestBody JSONObject request){
    	
    	LOGGER.info("Start efsFilesList() Method");
    	JSONArray jsonArr = null;
    	try {
	            String srcLoc = (String)request.get("efsLoc");	
    			jsonArr=  utils.efsFilesList(srcLoc);
    	}catch(Exception ex) 
    	{
    		LOGGER.error(FAILURE+" During efsFiles fetch: " , ex);		
    	}
    	LOGGER.info("End efsFilesList() Method");
    	return jsonArr;
    	
    }
    
    @PostMapping(DEPENDENCY_CHECK)
    public String jobDependencyCheck(@RequestBody JSONObject request, HttpServletResponse httpResp){
    	
    	LOGGER.info("Start jobDependencyCheck() Method");
    	String resp = "";
    	List<String> fileList = new <String>ArrayList();
    	try {
    			fileList = (List<String>) request.get("fileName");
	            String srcLoc = (String)request.get("sourceLoc");	
    			resp=  utils.jobDependencyCheck(fileList,srcLoc);
    	}catch(Exception ex) 
    	{
    		
    		if(ex.getMessage().contains("files notfound"))
    		{ 
    			LOGGER.info("S3 to EFS file transfer in progress");
    			resp = "S3 to EFS file transfer in progress" ;
    		
    		}
    		else
    		{
    			LOGGER.error(FAILURE+" During dependency check in EFS: " , ex);
    			resp = FAILURE+" During dependency check in EFS S3" ;
    			httpResp.setStatus(500);
    		}	
    				
    		
    	}
    	LOGGER.info("End jobDependencyCheck() Method");
    	return resp;
    	
    }
    
    @PostMapping(S3_TO_EFS)
    public BatchResponse filesMoveS3toEFS(@RequestBody JSONObject request){
    	
    	LOGGER.info("Start filesMoveS3toEFS() Method");
    	BatchResponse resp = null;
    	final BatchResponse respAsync = new BatchResponse();
    	final List<String> fileList;//new <String>ArrayList();
    	try {
    			fileList = (List<String>) request.get("fileName");
	            String targetLoc = (String)request.get("targetLoc");	
	            String callType = (String)request.get("callType");
	            
	            if(callType!=null && callType.equals("async"))
	            {
	          	  
	  		        	  CompletableFuture<String> future = CompletableFuture.supplyAsync(()-> 
	  		              {
	  		            	try {
	  		            		utils.dependencyFileMoveS3toEFS(fileList,targetLoc);
							} catch (Exception ex) {
								 LOGGER.error(FAILURE+" During Async filesMoveS3toEFS: " , ex);
								 respAsync.setResponseCode("500");
								 respAsync.setResponseName("ERROR");
								 respAsync.setResponseDescription("S3 files transfer failed" );
							}
	  		            	respAsync.setResponseCode("200");
	  		        		respAsync.setResponseName(SUCCESS);
	  		        		respAsync.setResponseDescription("Async file Transfer from S3 Inprogress.... " );
	  		        	  	
	  		        	  	return "Async file Transfer from S3 Inprogress.... ";
	  		              });
	  		        	  future.thenAccept(result->{ 		  
	  		        		respAsync.setResponseCode("200");
	  		        		respAsync.setResponseName(SUCCESS);
	  		        		respAsync.setResponseDescription(result );
	  		          	  });
	  		        	  resp = respAsync;
	            }	     
	            else
	             resp=  utils.dependencyFileMoveS3toEFS(fileList,targetLoc);
    			
    	}catch(Exception ex) 
    	{
    		LOGGER.error(FAILURE+" During filesMoveS3toEFS: " , ex);
    			  resp.setResponseCode("500");
		          resp.setResponseName("ERROR");
		          resp.setResponseDescription("S3 files transfer failed" );
    	}
    	LOGGER.info("End filesMoveS3toEFS() Method");
    	return resp;
    	
    }
    
    @PostMapping(CALL_BATCH)
    public @ResponseBody BatchResponse callBatchDetails(@RequestBody JSONObject request)
    {
    	 LOGGER.info("callBatchDetails() Method Started");
    	  String jobId = (String)request.get("jobID");		
          String jclConfPath = (String)request.get("jclConfPath");	
          String jobPath = (String)request.get("jobPath");
          String batchType = (String)request.get("batchType");
       
         
          BatchResponse resp= new BatchResponse();
           resp = services.batchExecute(jobId, jclConfPath,jobPath);
           
           if(batchType!=null && batchType.equalsIgnoreCase("actual"))
           {
        	   boolean flag =services.contentFileMovetoS3(jobId);
        	   if(flag)
        	   LOGGER.info(jobId+": Content file moved to S3 successfully");
           }  
                 	   
        LOGGER.info("callBatchDetails() Method Ended"); 
      return resp;
    }  
