package rover.control;

public class IMU extends Thread {
	
	/* References to arduino
	
	   Pin Mapping: http://arduino.cc/en/Hacking/PinMapping
	   Methods: 	http://arduino.cc/en/Reference/
	
	   Hardware Setup:
	   Acc_Gyro <--->  Arduino
	   5V       <--->  5V  
	   GND      <--->  GND
	   AX       <--->  AN0
	   AY       <--->  AN1
	   AZ       <--->  AN2
	   GX4      <--->  AN3  
	   GY4      <--->  AN4  
	*/
	
	// Global Variables
	static int INPUT_COUNT = 5;     //number of analog inputs
	float VDD = 5000.0f;       		//Analog reference voltage in milivolts
	double PI = 3.14159265358979f;
	
	
	int an[];      					//analog inputs  
	int firstSample;	 			//marks first sample
	
	//Notation "w" stands for one of the axes, so for example RwAcc[0],RwAcc[1],RwAcc[2] means RxAcc,RyAcc,RzAcc
	//Variables below must be global (their previous value is used in getEstimatedInclination)
	float RwEst[];     //Rw estimated from combining RwAcc and RwGyro
	long lastMicros;  

	//Variables below don't need to be global but we expose them for debug purposes
	long interval; //interval since previous analog samples
	float RwAcc[];         //projection of normalized gravitation force vector on x/y/z axis, as measured by accelerometer
	float RwGyro[];        //Rw obtained from last estimated value and gyro movement
	float Awz[];           //angles between projection of R on XZ/YZ plane and Z axis (deg)
	
	
	// constructor
	// initialize some variables
	IMU() {
		an = new int [INPUT_COUNT];	// analog inputs
		
		RwEst = new float [3];
		lastMicros = 0;  

		interval = 0;
		
		RwAcc = new float [3];
		RwGyro = new float [3];
		Awz = new float [2];
	}		
	
	
	// Guide: setup method in arduino - http://arduino.cc/en/Reference/setup
	//void setup() {
	public void run() {  
		  int i;
		  
		  // Guide - what is Serial.begin? Sets the data rate in bits per sec for serial data transmission
		  // http://arduino.cc/en/Serial/Begin
		  // ignore for now because IOIO board may already have this setup by default
		  
		  //Serial.begin(57600); 

		  // Initialize config Class
		  Config config = new Config(INPUT_COUNT);
		  
		  //Setup parameters for Acc_Gyro board, see http://www.gadgetgangster.com/213
		  for(i=0;i<=2;i++){                  // X,Y,Z axis
		    config.zeroLevel[i] = 1650;       // Accelerometer zero level (mV) @ 0 G
		    config.inpSens[i] = 478;          // Accelerometer Sensisitivity mV/g
		  }        
		  
		  for(i=3;i<=4;i++){
		    config.inpSens[i] = 2000;	    // Gyro Sensitivity mV/deg/ms    
		    config.zeroLevel[i] = 1230;     // Gyro Zero Level (mV) @ 0 deg/s  
		  }

		  config.inpInvert[0] = 1;  //Acc X
		  config.inpInvert[1] = 1;  //Acc Y
		  config.inpInvert[2] = 1;  //Acc Z

		  //Gyro readings are inverted according to accelerometer coordonate system
		  //see http://starlino.com/imu_guide.html for discussion
		  //also see http://www.gadgetgangster.com/213 for graphical diagrams
		  config.inpInvert[3] = 1;  //Gyro X  
		  config.inpInvert[4] = 1;  //Gyro Y

		  config.wGyro = 10;
		  
		  firstSample = 1;
		  
		  // Custom: manually call loop function
		  // Loop function is called consecutively without any interrupts
		  while (true) {
			  loop(config);
		  }
	}
	
	// Guide: loop method in arduino - http://arduino.cc/en/Reference/loop
	public void loop(Config c) {
		  getEstimatedInclination(c);
		  
		  // added custom sleep to loop method
		  try{
			  Thread.sleep(1000);
			}catch(InterruptedException ex){
			  //do stuff
			}
	}
	
	void getEstimatedInclination(Config config){
		  
		// remove static from all variables
		int i = 0;
		int w = 0;
		float tmpf = 0;
		float tmpf2 = 0;  
		long newMicros = 0; //new timestamp
		char signRzGyro;  

		//get raw adc readings
		
		// Guide: what is micros? return time in millisec - http://arduino.cc/en/Reference/micros
		//newMicros = micros();       //save the time when sample is taken 
		newMicros = System.currentTimeMillis();
		
		for(i=0;i<INPUT_COUNT;i++)
			
			// Guide: what is analogRead? get pin values of each analog - http://arduino.cc/en/Reference/analogRead
			an[i]= (int)ServoControlActivity.getSensorReadings(i);
		
			//compute interval since last sampling time
			interval = newMicros - lastMicros;    //please note that overflows are ok, since for example 0x0001 - 0x00FE will be equal to 2 
			lastMicros = newMicros;               //save for next loop, please note interval will be invalid in first sample but we don't use it
		  
			// get accelerometer readings in g, gives us RwAcc vector
			for(w=0;w<=2;w++) RwAcc[w] = ServoControlActivity.getSensorReadings(w);
		  
			//normalize vector (convert to a vector with same direction and with length 1)
			normalize3DVector(RwAcc);
		  
			if (firstSample > 0){
				for(w=0;w<=2;w++) RwEst[w] = RwAcc[w];    //initialize with accelerometer readings
			
			} else {
				
				// evaluate RwGyro vector
				if(Math.abs(RwEst[2]) < 0.1) {
					//Rz is too small and because it is used as reference for computing Axz, Ayz it's error fluctuations will amplify leading to bad results
					//in this case skip the gyro data and just use previous estimate
					for(w=0;w<=2;w++) RwGyro[w] = RwEst[w];
		    
				} else {

					//get angles between projection of R on ZX/ZY plane and Z axis, based on last RwEst   
					for(w=0;w<=1;w++) {
		        		tmpf = 1;//getInput(3 + w);                         //get current gyro rate in deg/ms
		        		tmpf *= interval / 1000.0f;                     //get angle change in deg
		        		Awz[w] = (float) (Math.atan2(RwEst[w],RwEst[2]) * 180 / PI);   //get angle and convert to degrees        
		        		Awz[w] += tmpf;                                 //get updated angle according to gyro movement
					}
					
					//estimate sign of RzGyro by looking in what qudrant the angle Axz is, 
					//RzGyro is positive if  Axz in range -90 ..90 => cos(Awz) >= 0
			      	int tempSignRzGyro = ( Math.cos(Awz[0] * PI / 180) >=0 ) ? 1 : -1;
			       	signRzGyro = (char) tempSignRzGyro;
		      	  
			       	//reverse calculation of RwGyro from Awz angles, for formula deductions see  http://starlino.com/imu_guide.html				  
			       	for(w=0;w<=1;w++){	    
			       		RwGyro[w] = (float) Math.sin(Awz[w] * PI / 180);
			    		RwGyro[w] /= Math.sqrt(1 + Math.pow(Math.cos(Awz[w] * PI / 180), 2) * Math.pow(Math.tan(Awz[1-w] * PI / 180), 2) );
			       	}
		      
			       	RwGyro[2] = (float) (signRzGyro * Math.sqrt(1 - Math.pow(RwGyro[0], 2) - Math.pow(RwGyro[1], 2)));  
			}
		 
				// combine Accelerometer and gyro readings		    
				for(w=0;w<=2;w++) RwEst[w] = (RwAcc[w] + config.wGyro* RwGyro[w]) / (1 + config.wGyro);
				normalize3DVector(RwEst);  
		}
		  
		firstSample = 0;
	}
	
	// custom: created own 3d Vector normalization
	// http://www.fundza.com/vectors/normalize/
	// http://snipd.net/2d-and-3d-vector-normalization-and-angle-calculation-in-c
	public void normalize3DVector(float a[]) {
		
		float length = 0;
		
		for(int i = 0; i < a.length; i++) {
			length += Math.pow(a[i], 2);
		}
		
		length = (float) Math.abs(Math.sqrt(length));
		
		for(int i = 0; i < a.length; i++) {
			a[i] = a[i] / length;
		}
	}
	
}
