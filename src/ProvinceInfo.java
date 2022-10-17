public class ProvinceInfo {
    // commenting out variables not needed to be used from the API
//  private String Country;  
//  private String CountryCode; 
//  private String City;
//  private String CityCode;
//  private String Lat;
//  private String Lon;
  private String Province;
  private int Confirmed;
  private int Deaths;
  private int Recovered;
  private int Active;
  private String Date;
  private int NewCases = 0;
  
  //Constructor
  public ProvinceInfo(String[] str) {
    Date = str[0];
    Province = str[1];
    Confirmed = Integer.parseInt(str[2]);
    Deaths = Integer.parseInt(str[3]);
    Recovered = Integer.parseInt(str[4]);
    Active = Integer.parseInt(str[5]);
    NewCases = Integer.parseInt(str[6]);
  }
  
  //Getters and Setters
  public String getProvince(){
    return Province;
  }
  public int getConfirmed() {
    return Confirmed;
  }  
  public int getDeaths() {
    return Deaths;
  }    
  public int getRecovered() {
    return Recovered;
  }   
  public int getActive() {
    return Active;
  }     
  public String getDate(){
    return Date;
  }  
  public int getNewCases() {
    return NewCases;
  }  
  public void setNewCases(int newCases) {
    this.NewCases = newCases;
  }
  
  // toString method used to output result that can be used in CSV format
  public String toString() {
    return Date + ", " + Province + ", " + Confirmed + ", " + Deaths + ", " + Recovered + ", " + Active + ", " + NewCases;
  }

  //overriding hashCode to use custom keys created for the hashSet
  @Override
  public int hashCode() {   
    String dateProvince = Date + Province;
    return dateProvince.hashCode();
    
  }
  
  //overriding default equals method when using .add() for hashSet
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    
    if (!(o instanceof ProvinceInfo)) {
      return false;
    }
    
    ProvinceInfo c = (ProvinceInfo) o;    
    return (this.Province.equals(c.getProvince()) 
        && (this.Date.equals(c.getDate()) )
        );
   
  }
}
