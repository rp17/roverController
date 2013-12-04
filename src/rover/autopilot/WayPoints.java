package rover.autopilot;

/**
 * AVOID CODE FOR NOW WE'LL START USING MULTIPLE
 * WAVEPOINTS LATER
 * 
 * 
 * @author Calvin
 *
 */

public class WayPoints {
	
	//Number of waypoints defined
	private final int waypoints = 6;
	
	
	public float wp_lat[] = new float[waypoints+1];
	public float wp_lon[] = new float[waypoints+1];
	public int wp_alt[]   = new int [waypoints+1];
	
	
	void setup_waypoints() {
	  /*Declaring waypoints*/

	  wp_lat[1]=  (float) 34.982613;
	  wp_lon[1]= (float) -118.443357; 
	  wp_alt[1]=50; //meters 

	  wp_lat[2]= (float) 34.025136;
	  wp_lon[2]=(float) -118.445254; 
	  wp_alt[2]=100; //meters

	  wp_lat[3]=(float) 34.018287;
	  wp_lon[3]=(float) -118.456048; 
	  wp_alt[3]=100; //meters

	  wp_lat[4]= (float) 34.009332;
	  wp_lon[4]=(float) -118.467672; 
	  wp_alt[4]=50; //meters

	  wp_lat[5]=  (float) 34.006476;
	  wp_lon[5]=(float) -118.465413; 
	  wp_alt[5]=50; //meters

	  wp_lat[6]=  (float) 34.009927;
	  wp_lon[6]= (float) -118.458320; 
	  wp_alt[6]= 20; //meters


	}
	
	
	

}
