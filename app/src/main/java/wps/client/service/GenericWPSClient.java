package wps.client.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;

import org.geotools.feature.FeatureCollection;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;


public class GenericWPSClient {
	
	String wpsURL;
	String wpsProcessID;
	HashMap<String, Object> wpsInputs;
	HashMap<String, Object> outputs;
	Map<String,Object> wpsOutputs;
	String catalogURL;
	FeatureCollection featureCollection;
	FeatureCollection inputFeatureCollection;
	
	
public GenericWPSClient(String wpsURL, String wpsProcessID, HashMap<String,Object> wpsInputs, String catalogURL){
	
	this.wpsURL = wpsURL;
	this.wpsProcessID = wpsProcessID;
	this.wpsInputs = wpsInputs;
	this.catalogURL = catalogURL;
	
	System.out.println("WPS URL " + wpsURL);
	System.out.println("WPS Process ID " + wpsProcessID);
	//WPSConfig.getInstance("res/wps_config_geotools.xml");
        try {
                ProcessDescriptionType describeProcessDocument = requestDescribeProcess(
                                wpsURL, wpsProcessID);
               // System.out.println(describeProcessDocument);
        } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        try {
                CapabilitiesDocument capabilitiesDocument = requestGetCapabilities(wpsURL);
                ProcessDescriptionType describeProcessDocument = requestDescribeProcess(
                                wpsURL, wpsProcessID);
                
                outputs = executeProcess(wpsURL, wpsProcessID,
                        describeProcessDocument, wpsInputs);

        } catch (WPSClientException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        } catch (Exception e) {
                e.printStackTrace();
        }
}

public CapabilitiesDocument requestGetCapabilities(String url)
                throws WPSClientException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();
       
        wpsClient.connect(url);

        CapabilitiesDocument capabilities = wpsClient.getWPSCaps(url);
        
        //System.out.println(capabilities.toString());

        ProcessBriefType[] processList = capabilities.getCapabilities()
                        .getProcessOfferings().getProcessArray();

        for (ProcessBriefType process : processList) {
              //  System.out.println(process.getIdentifier().getStringValue());
        }
        return capabilities;
}

public ProcessDescriptionType requestDescribeProcess(String url,
                String processID) throws IOException {

        WPSClientSession wpsClient = WPSClientSession.getInstance();

        ProcessDescriptionType processDescription = wpsClient
                        .getProcessDescription(url, processID);

        InputDescriptionType[] inputList = processDescription.getDataInputs()
                        .getInputArray();

        for (InputDescriptionType input : inputList) {
                System.out.println(input.getIdentifier().getStringValue());
        }
        return processDescription;
}

public HashMap<String, Object> executeProcess(String url, String processID,
                ProcessDescriptionType processDescription, HashMap<String, Object> inputs)  {

    org.n52.wps.client.ExecuteRequestBuilder executeBuilder = new org.n52.wps.client.ExecuteRequestBuilder(
                        processDescription);

    HashMap <String, Object> result = new HashMap<String, Object>();
        
        for (InputDescriptionType input : processDescription.getDataInputs()
                        .getInputArray()) {


                String inputName = input.getIdentifier().getStringValue();



                Object inputValue = inputs.get(inputName);
               
      
              
      
                if (input.getLiteralData() != null) {
                        if (inputValue instanceof String) {
                                executeBuilder.addLiteralData(inputName,
                                                (String) inputValue);
                        }
                } else if (input.getComplexData() != null) {
                	System.out.println("Generic WPS Client HERE 3 " + inputName + " " + inputValue + " " + inputValue.getClass());
                	//System.out.println("Here 4 " + inputValue.toString());
                        // Complexdata by value
                        if (inputValue instanceof FeatureCollection || inputValue instanceof GTVectorDataBinding) {
                        	System.out.println("instance of FeatureCollection || ObjectDataType " + inputName);
                                //IData data = new GTVectorDataBinding(
                                  //              (FeatureCollection) inputValue);
                                IData data = (IData) inputValue;
                                try {
									executeBuilder
									                .addComplexData(
									                                (String) inputName,
									                                data,
									                                "http://schemas.opengis.net/gml/3.1.0/base/feature.xsd",
									                                null, "text/xml; subtype=gml/3.1.0");
								} catch (WPSClientException e) {
									System.out.println("add complex data exception " + e);
									e.printStackTrace();
								}
                        }
                        // Complexdata Reference
                        if (inputValue instanceof String) {
                        	System.out.println("instance of string " + inputName);
                                executeBuilder
                                                .addComplexDataReference(
                                                                inputName,
                                                                (String) inputValue,
                                                                "http://schemas.opengis.net/gml/3.1.0/base/feature.xsd",
                                                                null, "text/xml; subtype=gml/3.1.0");
                        }

                       
                }
                if (inputValue == null && input.getMinOccurs().intValue() > 0) {
                    try {
						throw new IOException("Property not set, but mandatory: "
						                + inputName);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            }
        }
       
        
        for (OutputDescriptionType output : processDescription.getProcessOutputs()
                .getOutputArray()) {
        	
        	
        	String outputName = output.getIdentifier().getStringValue();
        	
        	
        	if (output.getComplexOutput() != null){

        		String mimeType = output.getComplexOutput().getSupported().getFormatArray(1).getMimeType();
        		executeBuilder.setMimeTypeForOutput(mimeType, outputName);
     
        		String schema = output.getComplexOutput().getSupported().getFormatArray(1).getSchema();
        		if(schema!=null){
        		
                executeBuilder.setSchemaForOutput(
                                schema,
                               outputName);
        		}
                System.out.println("outputName " + outputName + " mimeType " + mimeType + " schema " + schema);
        		
        	}
        	
        
     
        	else if (output.getLiteralOutput() != null) {
                  
            
            
        	}
        }
       
       
      
     /**   executeBuilder.setMimeTypeForOutput("text/xml; subtype=gml/3.1.0", "result");
        executeBuilder.setSchemaForOutput(
                        "http://schemas.opengis.net/gml/3.1.0/base/feature.xsd",
                        "result");
        
        executeBuilder.setMimeTypeForOutput("text/xml; subtype=gml/3.1.0", "qual_result");
        executeBuilder.setSchemaForOutput(
                        "http://schemas.opengis.net/gml/3.1.0/base/feature.xsd",
                        "qual_result");**/
        
        //executeBuilder.setMimeTypeForOutput("text/plain", "metadata"); 
           
        ExecuteDocument execute = executeBuilder.getExecute();
        execute.getExecute().setService("WPS");
        WPSClientSession wpsClient = WPSClientSession.getInstance();
        
        Object responseObject;
		try {
			responseObject = wpsClient.execute(url, execute);
		
       
        if (responseObject instanceof ExecuteResponseDocument) {
                ExecuteResponseDocument response = (ExecuteResponseDocument) responseObject;
                ExecuteResponseAnalyser analyser = new ExecuteResponseAnalyser(
                                execute, response, processDescription);
                System.out.println("HERE 6");
                
               
                
                
                try{
                for (OutputDescriptionType output : processDescription.getProcessOutputs()
                        .getOutputArray()) {
                	
                	String outputName = output.getIdentifier().getStringValue();
                	Object outputValue = analyser.getComplexData(outputName, GTVectorDataBinding.class);
                	System.out.println("HERE 7 " + outputName + " " + outputValue);
                	FeatureCollection tempF = ((GTVectorDataBinding) outputValue).getPayload();
                	
                	
                	if(outputValue != null && outputValue instanceof GTVectorDataBinding){
                		System.out.println("HERE 8 output name " + outputName + " outputValue size " + tempF.size());
                		result.put(outputName, outputValue);
                		
                	}
                	
                	else if (output.getLiteralOutput()!=null){
                		
                		Object literalOutput = output.getLiteralOutput();
                		result.put(outputName, literalOutput);
                	}
                	
                	
                	
                	
                }
                
                }catch(RuntimeException e){
                	System.out.println("Error getting output data " + e );
                }
        }
        } catch (WPSClientException e1) {
			System.out.println("error generating response object " + e1);
			e1.printStackTrace();
		}
		
                
                                             
                /**Object data =  analyser.getComplexData("result",
                        GTVectorDataBinding.class);
                
                result.put("result", data);
                
                Object data2 =null;
                
                if( analyser.getComplexData("qual_result", GTVectorDataBinding.class)!=null){
                
                		data2 = analyser.getComplexData("qual_result", 
                			GTVectorDataBinding.class);
                }
                
                System.out.println("HERE 6 " + data2.toString());
                result.put("qual_result", data2);**/
                
                System.out.println("result collection size " + result.size());
                return result;
        }
		


public HashMap<String, Object> getOutputs(){
	
	return outputs;
	
}

private File parseXMLFromWPS(GenericFileData xmlGenericData){
	  
	   File file =  xmlGenericData.getBaseFile(true);
	   
	   InputStream fis;
	try {
		fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
	 
		    File newFile = File.createTempFile("temp2", "xml");
		    
		    FileWriter fw = new FileWriter(newFile);
		     
		     for (String line = br.readLine(); line != null; line = br.readLine()) {                               
		         String newLine = line.replaceAll("&gt;",">").replaceAll("&lt;", "<").replaceAll("&amp;","&");                               
		         System.out.println("NEWLINE " + newLine);          
		         fw.write(newLine);

		      }
		     fw.close();
		     fis.close();
		     return newFile;
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
     
  

	
	return null;
  	  
}

}

