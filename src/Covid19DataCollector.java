import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;


import com.google.gson.Gson;

public class Covid19DataCollector {
  
  private final static String FILENAME = "Covid19APIResults.csv";
  private final static LocalDateTime CURRTIME = LocalDateTime.now();
  private final static DateTimeFormatter CURRTIMEFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
  
  public static void main(String[] args) {
    String startDate = "";
    String endDate = "";
  
    if (args.length == 2) {
      startDate = args[0];
      endDate = args[1];
    } else if (args.length == 0){
      // if there is no dates input, grab current date as end date and last week as start date
      endDate = CURRTIME.format(CURRTIMEFORMAT);

      LocalDateTime pastTime = CURRTIME.minusDays(7);
      startDate = pastTime.format(CURRTIMEFORMAT);

    } else {
      System.out.println("Provide either two valid dates or zero");
      System.out.println("Inputs provided: " + startDate + " " + endDate);
      return;
    }
   
    if (areDatesValid(startDate, endDate)) {
      writeCSVFile(createFilteredProvinceInfoArray(startDate, endDate));   
    } 
  }
  
  //Method to validate date inputs given with boolean
  public static boolean areDatesValid (String startDate, String endDate) {
    System.out.println("Inputs provided: " + startDate + " " + endDate);
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      
      sdf.parse(startDate);
      sdf.parse(endDate);
      
      // return the FALSE conditions   
      if (endDate.compareTo(CURRTIME.format(CURRTIMEFORMAT)) > 0){
        System.out.println("Invalid Arguments: endDate is in the future");
        return false;
      } else if (endDate.compareTo(startDate) == 0) {
        System.out.println("Invalid Arguments: startDate is same as endDate");
        return false;
      } else if ( startDate.compareTo(endDate ) > 0 ) {  //sdf.parse(endDate).before(sdf.parse(startDate))) { 
        System.out.println("Invalid Arguments: Dates Provided are not in chronological order");
        return false;
      }      
    } catch (ParseException e) {
      System.out.println("Invalid Arguments: Dates provided are not in a valid format");
      return false;
    }   
    return true;
  }
  
  //Method to create a filtered array of provinceInfo objects using start + end dates given to connect to API
  public static ProvinceInfo[] createFilteredProvinceInfoArray(String startDate,String endDate) {
    
    ProvinceInfo[] returnArray = {};
    
    //grab the start date MINUS ONE so that you can get new confirmed cases on the first date provided
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    LocalDateTime startDateMinusOne = LocalDateTime.parse(startDate,formatter).minusDays(1).truncatedTo(ChronoUnit.DAYS);
    String startDateMinusOneString = startDateMinusOne.format(formatter);
 
    try {
      // Creating a connection to covid19api & checking that connection is successful     
      URL url = new URL("https://api.covid19api.com/country/canada?from="+startDateMinusOneString+"&to="+endDate);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      // if response code is good, continue with method
      if (conn.getResponseCode() != 200) {
        System.out.println("Could not connect to Covid19api");
        throw new RuntimeException();
      }
     
      //create an array of ProvinceInfo objects using Gson to parse the covid19API
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      Gson gson = new Gson();    
      ProvinceInfo[] pi = gson.fromJson(in, ProvinceInfo[].class);
      
      //sort the array using Arrays to sort by Provinces alphabetically
      Arrays.sort(pi, (a,b) -> a.getProvince().compareTo(b.getProvince()));
         
      //check if the previous entry is the same province first
      //then you set the new cases variable to equal the current day's confirmed cases minus the previous day 
      for (int i = 1; i < pi.length; i++) {
        if (pi[i-1].getProvince().equals(pi[i].getProvince())) {
          pi[i].setNewCases((pi[i].getConfirmed()) - (pi[i-1].getConfirmed()));
        }
      }
      
      //filter out results of the extra day (startDateMinusOne) as well as the results with an empty province (used for total of canada)
      returnArray = Arrays.stream(pi)
                          .filter(c -> !c.getDate().equals(startDateMinusOneString) && !c.getProvince().equals(""))                       
                          .toArray(ProvinceInfo[]::new);     
           
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }    
    return returnArray;
  }
  
  //writing into a CSV file
  static void writeCSVFile (ProvinceInfo[] listOfProvinceInfo) {
    
    if (listOfProvinceInfo == null) {
      System.out.println("Cannot use a null array");
      return;
    }
    
    ProvinceInfo[] mergedList = listOfProvinceInfo; 
    try {

      File csvFile = new File(FILENAME);
      
      // create file if new, otherwise delete old file and create a new one
      if (csvFile.exists()) {
        // if the file exists, take the old input data and merge with new input data
        mergedList = mergeProvinceInfoArray(storeOldArray(csvFile),listOfProvinceInfo);
        System.out.println("Overwriting File: " + csvFile.getName());
      } else {
        System.out.println("Created New File: " + csvFile.getName());
      }  
        
      //given ProvinceInfo array, write down values into CSV file with proper headers
      FileWriter dataWriter = new FileWriter(csvFile);
      dataWriter.write("Date, Province, Confirmed, Deaths, Recovered, Active, New Cases\n");
        for (ProvinceInfo fl : mergedList) {
          dataWriter.append(fl.toString() + "\n");
        }         
      dataWriter.flush();
      dataWriter.close(); 
      
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }
  // If there is a CSV file already created, stores values already recorded into an array
  public static ProvinceInfo[] storeOldArray (File fileName) {
    
    ProvinceInfo[] oldArray = {};
    try {
      String csvSplitBy = ", ";
      String csvLine = "";
      int lineCount = 0;
      int index = 0;
      
      BufferedReader dataReader = new BufferedReader(new FileReader(FILENAME));
      BufferedReader lineCounter = new BufferedReader(new FileReader(FILENAME));
      
      // loop through each line to get number of lines and array list is of size equal to line count minus one for array index
      while ((csvLine = lineCounter.readLine())!= null) { 
        lineCount++;
      }
      oldArray = new ProvinceInfo[lineCount-1]; 
      lineCounter.close();
      
      // skip the headers
      dataReader.readLine(); 
                  
      // loop through each line of the file and add a new province object into the oldArray
      while ((csvLine = dataReader.readLine())!= null) {
        ProvinceInfo piLineData = new ProvinceInfo(csvLine.split(csvSplitBy));
       oldArray[index] = piLineData;
       index++;
      }
      
      dataReader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }catch (IOException e) {
      e.printStackTrace();
    }

    return oldArray;
    }
  
  //merge both new and old arrays by using a HashSet and remove duplicate entries
  public static ProvinceInfo[] mergeProvinceInfoArray (ProvinceInfo[] oldArray, ProvinceInfo[] newArray) {

    HashSet<ProvinceInfo> hashpi = new HashSet<ProvinceInfo>();
     for (ProvinceInfo piObj : newArray) {
       hashpi.add(piObj);
     }  
     for (ProvinceInfo piObj : oldArray) {
       hashpi.add(piObj);
     }

    ProvinceInfo[] reducedArray = new ProvinceInfo[hashpi.size()];
    hashpi.toArray(reducedArray);
    
    // sorting the new reduced array
    Arrays.sort(reducedArray, (a,b) -> a.getDate().compareTo(b.getDate()));
    Arrays.sort(reducedArray, (a,b) -> a.getProvince().compareTo(b.getProvince()));
    
    return reducedArray;
  }    
}

