package rover.autopilot;

/**
 * AVOID CODE ITS NOT USED FOR ANDROID APPLICAITON
 * 
 * MOST LIKELY WE'LL USE LATER DOWN THE ROAD WHEN IMPLEMENTING
 * 
 * PID FOR ALTITUDE
 * 
 * 
 */



import rover.pid.PIDControl;
import rover.gps.GPS;

public class Start {
	
	// PID max and mins
	public static final int heading_min = -30;
	public static final int heading_max = 30;
	
	public static final int altitude_max = 40;
	public static final int altitude_min = -45;
	
	
	//PID gains
	//At the begining try to use only proportional.. 
	//The original configuration works fine in my simulator.. 
	static final int 		Kp_heading = 10;
	static final float	 	Ki_heading = (float) 0.01;
	static final float		Kd_heading = (float) 0.001;

	static final int Kp_altitude = 4;
	static final float Ki_altitude = (float) 0.001;
	static final int Kd_altitude = 2;
	
	//PID loop variables
	public static int heading_previous_error; 
	public static float heading_I; //Stores the result of the integrator
	static int altitude_previous_error;
	static float altitude_I; //Stores the result of the integrator
	
	
	//PID K constants, defined at the begining of the code
	public static final float  kp[]={Kp_heading,Kp_altitude};	
	public static final float  ki[]={Ki_heading,Ki_altitude};	 
	public static final float  kd[]={Kd_heading,Kd_altitude};
	
	public static int reverse_yaw = 1; // normal = 0 and reverse = 1
	
	
	// GPS obtained information
	// byte fix_position=0;//Valid gps position
	public float lat = 0; //Current Latitude
	public float lon = 0; //Current Longitude
	// byte ground_speed=0; //Ground speed? yes Ground Speed.
	public int course = 0; // Course over ground...
	// int alt=0; //Altitude,
	
	
	
	public int wp_distance = 0; // Stores the distances from the current waypoint
	public int wp_bearing = 0; // Stores the bearing from the current waypoint

	
	int current_wp = 1; //This variables stores the actual waypoint we are trying to reach.. 
	
	int test = 0;

	/*************************************************************************
	 * Main Function, reads gps info, calculates navigation, executes PID and sends values to the servo 
	 *************************************************************************/	
	
	WayPoints wp = new WayPoints();
	GPS gps = new GPS();
	PIDControl pc = new PIDControl();
	
	public float course(int lat, int lon, int current_wp_lat, int current_wp_lon) {

		// Calculating Bearing, this function is located in the GPS.java class 
		wp_bearing = gps.get_gps_course(lat, lon, wp.wp_lat[current_wp], wp.wp_lon[current_wp]);
		
		return wp_bearing;
		
		
	}
	
	float gpsdistanc(int lat, int lon, int current_wp_lat, int current_wp_lon) {
		
		// Calculating Distance, this function is located in the GPS.java class
		//wp_distance = gps.get_gps_dist(lat, lon, wp.wp_lat[current_wp], wp.wp_lon[current_wp]);
		
		return wp_distance;
		
	}
	
	
	public Start() {}
	
	public int get_crossTrackError(int desiredCourse, int currentCourse) {
		
		int error = pc.PID_heading(pc.compass_error(desiredCourse, currentCourse));
		return error;
		
	}
	
	public int getPID(int desiredCourse, int currentCourse) {
				
		
		
		// Central Position + PID(compass_error(desired course, current course)). 
		test = pc.PID_heading(pc.compass_error(desiredCourse, currentCourse)); 

		return test;
		// Now if you wanted to test and only go east try the following code:
		// instead of line above:
		// test = middle_yaw+PID_heading(compass_error(45, course));
		
		
		// Now send "central position" midle_object data to servo
		// pulse_servo_yaw(test); // Sending values to servo, 90 degrees is central position.
		
		
		// Look at PID for altitude
		// int PID_altitude(int PID_set_Point, int PID_current_Point);
		
		// System.out.println(test);
		
	}
	
	
}
