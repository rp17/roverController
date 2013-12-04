package rover.gps;

public class GPS {

	
	/*************************************************************************
	 * //Function to calculate the course between two waypoints
	 * //I'm using the real formulas--no lookup table fakes!
	 *************************************************************************/
	public int get_gps_course(float flat1, float flon1, float flat2, float flon2)
	{
		float calc;
		float bear_calc;

		float x = (69.1f * (flat2 - flat1)); 
		float y = (69.1f * (flon2 - flon1) * (float)Math.cos(flat1*0.01745200698080f));

		calc = (float)Math.atan2(y,x);

		// Convert to degrees added code by: Calvin Carter	
		// bear_calc= degrees(calc);
    
		bear_calc = calc*57.29577951308232f; // 180/PI == 57.29...
      
		// end of added code convert to degrees
	  
		if(bear_calc <= 1){
			bear_calc = 360 + bear_calc; 
		}
	  
		return (int) bear_calc;
	}


	/*************************************************************************
	 * //Function to calculate the distance between two waypoints
	 * //I'm using the real formulas
	 *************************************************************************/
	
	// changed it to return float instead of int for better accuracy
	// by Calvin Carter 11/16/13
	public float get_gps_dist(float flat1, float flon1, float flat2, float flon2)
	{
		float x = (69.1f * (flat2 - flat1)); 
		float y = (69.1f * (flon2 - flon1) * (float)Math.cos(flat1*0.01745200698080f));
		return ((float)(Math.sqrt((x*x) + (y*y))))*1609.344f; 
	}
}
