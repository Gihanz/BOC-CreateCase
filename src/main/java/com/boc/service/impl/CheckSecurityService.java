

package com.boc.service.impl;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.log4j.Logger;

import com.boc.connector.CMConnector;
import com.boc.service.exceptions.BSLException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sun.net.www.http.HttpClient;

/*
Created By SaiMadan on Jul 1, 2016
*/
public class CheckSecurityService 
{
	public static Logger log = Logger.getLogger("com.boc.service.impl.CheckSecurityService");
	static ResourceBundle configMsgBundle = ResourceBundle.getBundle("config",Locale.getDefault());
	public String getCreateCase(String token,Object caseParameters,String branchCodeKey,String productCodeKey,String restURL,String caseType) throws BSLException,Exception
	{
		String referenceNumber = null;
		String rlcCode=null,rlcName=null,areaCode=null,areaName=null,provinceCode=null,provinceName=null;
			String var= null,rlcCodeValue=null,rlcNameValue=null,areaCodeValue=null,areaNameValue=null,provinceCodeValue=null,provinceNameValue=null;
			JSONObject jObject=null,areaProvincejObject=null;
			String restgetReferenceNumberOperation = configMsgBundle.getString("OPERATIONGETREFERENCE");
			String restgetAreaBranchOperation = configMsgBundle.getString("OPERATIONBRANCHAREA");
			rlcCode = configMsgBundle.getString("CMRLCCODE");
			rlcName=configMsgBundle.getString("CMRLCNAME");
			areaCode=configMsgBundle.getString("CMAREACODE");
			areaName=configMsgBundle.getString("CMAREANAME");
			provinceCode=configMsgBundle.getString("CMPROVINCECODE");
			provinceName=configMsgBundle.getString("CMPROVINCENAME");
			try {
				jObject = new JSONObject(caseParameters);
				if(jObject.getString(branchCodeKey).isEmpty())
				{
					log.error("Branch Code value is empty, sent as input from web");
					throw new BSLException("er.db.branchCodeNotFound");
				}
				if(jObject.getString(productCodeKey).isEmpty())
				{
					log.error("Product Name value is empty, sent as input from web");
					throw new BSLException("er.db.productNotFound");
				}
				String branchCode = jObject.getString(branchCodeKey);
				if(null==branchCode)
				{
					log.error("Branch Code value is null, sent as input from web");
					throw new BSLException("er.db.branchCodeNull");
				}
				String productCode = jObject.getString(productCodeKey);
				if(null==productCode)
				{
					log.error("Product Name value is null, sent as input from web");
					throw new BSLException("er.db.productNull");
				}
				referenceNumber = getReferenceNumber(branchCode,productCode,restURL,restgetReferenceNumberOperation);
				if(null==referenceNumber)
				{
					log.error("Unable to generate reference number,either Branch Code or Product name is invalid ");
					throw new BSLException("er.db.referenceNumberNotFound");
				}
				log.info("referenceNumber obtained is "+referenceNumber);
				
				String areaProvince = getAreaProvinceByBranch(branchCode,restURL,restgetAreaBranchOperation);
				System.out.println("areaProvince is "+areaProvince);
				log.info("areaProvince is "+areaProvince);
				areaProvincejObject = new JSONObject(areaProvince);
				rlcCodeValue = areaProvincejObject.getString("rlcCode");
				rlcNameValue = areaProvincejObject.getString("rlcName");
				areaCodeValue = areaProvincejObject.getString("areaCode");
				areaNameValue = areaProvincejObject.getString("areaName");
				provinceCodeValue = areaProvincejObject.getString("provinceCode");
				provinceNameValue = areaProvincejObject.getString("provinceName");
				
				jObject.put(rlcCode, rlcCodeValue);
				jObject.put(rlcName, rlcNameValue);
				jObject.put(areaCode, areaCodeValue);
				jObject.put(areaName, areaNameValue);
				jObject.put(provinceCode, provinceCodeValue);
				jObject.put(provinceName, provinceNameValue);
				caseParameters = jObject.toString();
				
				JsonParser parser = new JsonParser();
				JsonObject jObjectCaseparameters  = parser.parse((String) caseParameters).getAsJsonObject();
				log.info("jObjectCaseparameters is "+jObjectCaseparameters);
				CMConnector cmConnector = new CMConnector();
				log.info("caseParameters are "+jObjectCaseparameters);
				var = cmConnector.createCase(jObjectCaseparameters,referenceNumber,caseType);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				log.error(e.fillInStackTrace());
				e.printStackTrace();
				log.error("Either Branch Code or ProductName is not sent as input from web");
				throw new BSLException("er.db.productbranchCodeJSONNotFound");
			}
			catch(BSLException e)
			{
				
				e.printStackTrace();
				log.error(e.fillInStackTrace());
				throw new BSLException(e.getMessage());
			}
			catch(Exception e)
			{
				e.printStackTrace();
				log.error(e.fillInStackTrace());
				throw e;
			}
			return referenceNumber;
	}
	
	public String getAreaProvinceByBranch(String branchCode,String restURL,String operation)
	{
		String output = null;
		StringBuffer outputStr = new StringBuffer(); 
		try 
		{
			if(null!= branchCode)
			{
				String restUrl = restURL+"/"+operation+"?branchCode="+branchCode;
				URL url = new URL(restUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setInstanceFollowRedirects( false );
				conn.setRequestMethod( "POST" );
				conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
				conn.setRequestProperty( "charset", "utf-8");
				conn.setUseCaches( false );
	
				BufferedReader br = new BufferedReader(new InputStreamReader(
						(conn.getInputStream())));
	
				
				log.info("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					outputStr.append(output);
					log.info(output);
				}
				if(null!=outputStr)
					output=outputStr.toString();
				conn.disconnect();
			}
			
		}catch(BSLException bse)
		{
			bse.printStackTrace();
			log.error(bse.getMessage()+""+bse.fillInStackTrace());
			log.error("Exception Occured, unable to getAreaProvince");
			throw new BSLException(bse.getMessage());
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("malformedURL, unable to getAreaProvince");
			throw new BSLException("er.db.malformedURL");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			throw new BSLException("er.db.protocalExceptionURL");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("IOException Occured, unable to getAreaProvince");
			throw new BSLException("er.db.ioexception");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("Exception Occured, unable to getAreaProvince");
			throw new BSLException("er.db.ioexception");
		}
		
		return output;
		
	}
	
	
	public String getReferenceNumber(String branchCode,String productName,String restURL,String operation) throws BSLException
	{
		
		String output = null;
		StringBuffer outputStr = new StringBuffer(); 
		try 
		{
			if(null!= branchCode && null!=productName)
			{
				String restUrl = restURL+"/"+operation+"?branchCode="+branchCode+"&productName="+URLEncoder.encode(productName, "UTF-8");
				URL url = new URL(restUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setInstanceFollowRedirects( false );
				conn.setRequestMethod( "POST" );
				conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
				conn.setRequestProperty( "charset", "utf-8");
				conn.setUseCaches( false );
	
				BufferedReader br = new BufferedReader(new InputStreamReader(
						(conn.getInputStream())));
	
				
				log.info("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					outputStr.append(output);
					log.info(output);
				}
				if(null!=outputStr)
					output=outputStr.toString();
				conn.disconnect();
			}
			
		}catch(BSLException bse)
		{
			bse.printStackTrace();
			log.error(bse.getMessage()+""+bse.fillInStackTrace());
			log.error("Exception Occured, unable to getReferenceNumber");
			throw new BSLException(bse.getMessage());
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("malformedURL, unable to getReferenceNumber");
			throw new BSLException("er.db.malformedURL");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			throw new BSLException("er.db.protocalExceptionURL");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("IOException Occured, unable to getReferenceNumber");
			throw new BSLException("er.db.ioexception");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error(e.getMessage()+""+e.fillInStackTrace());
			log.error("Exception Occured, unable to getReferenceNumber");
			throw new BSLException("er.db.ioexception");
		}
		
		return output;
		
	}
}
